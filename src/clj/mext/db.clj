(ns mext.db
  (:require [crux.api :as crux]
            [taoensso.timbre :as timbre]
            [system.repl :refer [system]]
            [mext.util :as util]))

(defn now []
  (java.util.Date.))

(defn get-entity [id]
  (let [sys (-> system :db :db)]
    (crux/entity (crux/db sys) id)))

(defn delete-entity [id]
  (crux/submit-tx
    (-> system :db :db)
    [[:crux.tx/delete id]]))

(defn get-entities [ids]
  (->>
    (crux/q (crux/db (-> system :db :db))
            {:find '[?e]
             :where '[[?e :crux.db/id ?]
                      [(get ?ids ?e)]]
             :args [{'?ids ids}]})
    (map first)
    (into #{})))

#_(get-entities #{:5vxBOzakDbJleNA1rbA7FQ})

(defn prepare-for-tx [data]
  (mapv (fn [d]
          [:crux.tx/put
           d]) data))

(defn insert-all [entities]
 (crux/submit-tx
      (-> system :db :db)
      (prepare-for-tx entities)))

(defn- entity-data
  ([entities]
   (entity-data (-> system :db :db) entities))
  ([system entities]
    (let [sys (partial (crux/db system))]
      (map #(crux/entity sys (first %)) entities))))


(defn get-headlines [{:keys [offset limit] :or {offset 0 limit 10}}]
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       {:find '[?e ?published-at]
                         :where '[[?e :mext.headline/published-at ?published-at]]
                         :order-by [['?published-at :desc]]
                         :limit limit
                         :offset offset
                         })))

(comment
  (get-headlines {:limit 30})
  (last (get-headlines {})))

(defn get-headline-source [title]
  (-> (clojure.string/split title #"-")
    last
    (clojure.string/trim)))

(comment
  (->>
    (map :mext.headline/title (get-headlines {:limit 5000 :offset 0}))
    (map get-headline-source)
    frequencies
    (map vec)
    (sort-by second #(compare %2 %1))))

(comment
  (def data '({:crux.db/id :99b71def3937f5dcdcd8a0dc713046d2,
               :mext.headline/author nil,
               :mext.headline/title "Molde Εναντίον Άρης LIVE | Live Ποδόσφαιρο - UEFA Europa League Qualification | 08-08-19 | gazzetta.gr - gazzetta.gr",
               :mext.headline/description "Molde Εναντίον Άρης: Παρακολουθήστε live την αναμέτρηση  και σχολιάστε τον αγώνα στο chat του gazzetta.",
               :mext.headline/url "https://news.google.com/__i/rss/rd/articles/CBMiLmh0dHA6Ly93d3cuZ2F6emV0dGEuZ3IvZ2FtZS1zdGF0cy8xODE1LzMxMzc3MTXSAQA?oc=5",
               :mext.headline/image "http://www.gazzetta.gr/sites/default/files/styles/ogimage_watermark/public/article/2019-08/2182777.jpg?itok=zCj52TBv",
               :mext.headline/published-at #inst"2019-08-08T19:00:00.000-00:00"}
              {:crux.db/id :035bb7d29386b13e3f584c0179c463b2,
               :mext.headline/author "SDNA Newsroom",
               :mext.headline/title "Καρντόσο: «Δεν μπορούσαμε να απανεύσουμε - SDNA",
               :mext.headline/description "Την άποψη ότι η νίκη κόντρα στην Κραϊόβα δεν ήταν εύκολη υπόθεση για την ΑΕΚ εξέφρασε ο προπονητής της, Μιγκέλ Καρντόσο. «Δεν μπορούσαμε να αναπνεύσουμε».",
               :mext.headline/url "https://news.google.com/__i/rss/rd/articles/CBMiYmh0dHBzOi8vd3d3LnNkbmEuZ3IvcG9kb3NmYWlyby9ldXJvcGEtbGVhZ3VlL2FydGljbGUvNjI1NzY4L2thcm50b3NvLWRlbi1tcG9yb3lzYW1lLW5hLWFwYW5leXNveW1l0gEA?oc=5",
               :mext.headline/image "https://www.sdna.gr/sites/default/files/styles/share/public/article/2019/8/08/68701925_2469693939984019_4509030673645830144_n.jpg?itok=TOwLvmXW",
               :mext.headline/published-at #inst"2019-08-08T18:32:06.000-00:00"})))

(defn get-headline-count []
  (count (crux/q (crux/db (-> system :db :db))
                 '{:find [e]
                   :where [[e :mext.headline/title ?]]})))

(comment
  (get-headline-count))

(defn get-headlines-with-text []
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       '{:find [e]
                         :where [[e :mext.headline/content-words ?]]})))

(comment
  (count (get-headlines-with-text)))

;; headline tags

(defn add-headline-tag [id tag]
  (if-let [headline (get-entity id)]
    (crux/submit-tx
      (-> system :db :db)
      [[:crux.tx/put
        (update-in headline [:mext.headline/tags] (fnil conj #{}) tag)]])))

(defn delete-headline-tag [id tag]
  (if-let [headline (get-entity id)]
    (crux/submit-tx
      (-> system :db :db)
      [[:crux.tx/put
        (update-in headline [:mext.headline/tags] (fnil disj #{}) tag)]])))

(comment
  (get-headlines {})
  (add-headline-tag :5924ce1e6deff71defe2e1a9985d0224 :yolo1)
  (delete-headline-tag :5924ce1e6deff71defe2e1a9985d0224 :yolo))

;; sentiment

(defn set-interaction [hl-id user-id type value]
  (if-let [headline (get-entity hl-id)]
    (let [id (keyword (util/md5 hl-id user-id type))
          s {:crux.db/id            id
             :mext.interaction/value value
             :mext.interaction/type   type
             :mext.interaction/created-at (now)
             :mext.user/id user-id
             :mext.headline/id      hl-id}]
      (crux/submit-tx
        (-> system :db :db)
        [[:crux.tx/put s]]))))

(defn get-interactions [hl-id user-id]
  (entity-data (-> system :db :db)
    (crux/q (crux/db (-> system :db :db))
      {:find '[?e ?interaction-type]
       :where '[[?e :mext.interaction/type ?interaction-type]
                [?e :mext.interaction/created-at ?created-at]
                [?e :mext.headline/id ?hl-id]
                [?e :mext.user/id ?user-id]]
       :order-by [['?created-at :desc]]
       :args [{'?hl-id hl-id '?user-id user-id}]})))

(defn get-user-interactions [user-id]
  (entity-data (-> system :db :db)
    (crux/q (crux/db (-> system :db :db))
      {:find '[?e ?interaction-type ?created-at]
       :where '[[?e :mext.interaction/type ?interaction-type]
                [?e :mext.interaction/created-at ?created-at]
                [?e :mext.user/id ?user-id]]
       :order-by [['?created-at :desc]]
       :args [{'?user-id user-id}]})))

(comment
  (get-user-interactions :cb6e0a48-2644-4225-a2fc-ebd2579720e7)
  (get-user-interactions :86c4449c-d77f-474c-ab5b-5c4f80b7a31c) ;; duuuuuu
  (get-user-interactions :45e97e72-7480-4323-b7ce-688e139aa904)
  (get-interactions :d8037414c54888ba734f09321df49a7b :a76109ef-545d-4ff1-bb49-b2812194af1b )
  (set-interaction :d8037414c54888ba734f09321df49a7b :a76109ef-545d-4ff1-bb49-b2812194af1b :yolo 1)
  (get-entity :d8037414c54888ba734f09321df49a7b)
  (now)
  (get-users))


;; users

(defn get-users []
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       '{:find [e]
                         :where [[e :mext.user/email ?]]})))

(defn get-user-by-email [email]
  (first
    (entity-data (-> system :db :db)
                 (crux/q (crux/db (-> system :db :db))
                         {:find '[?e]
                          :where '[[?e :mext.user/email ?email]]
                          :args [{'?email email}]}))))

(defn insert-user [email]
  (crux/submit-tx
    (-> system :db :db)
    [[:crux.tx/put
      {:crux.db/id (keyword (util/uuid))
       :mext.user/email email}]]))

#_(get-users)


;; ok, pretty bad but it works TODO
(defn upsert-user [email]
  (if-let [user (get-user-by-email email)]
    user
    (do (insert-user email)
        (loop [i 0]
          (if-let [u (get-user-by-email email)]
            u
            (do (Thread/sleep 50)
                (when (< i 20)
                  (recur (inc i)))))))))
