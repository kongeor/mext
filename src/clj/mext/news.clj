(ns mext.news
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [mext.db :as db]
            [mext.util :as util]))

(defn fetch-latest-headlines []
  (if-let [token (:news-api-key env)]
    (let [{:keys [status error body] :as res} (client/get (str "https://newsapi.org/v2/top-headlines?country=gr&pageSize=30&apiKey=" token))]
      (if (= status 200)
        (json/parse-string body true)
        (timbre/error "Could not get latest headlines" status error body)))
    (timbre/warn "News api key not specified ... not fetching headlines")))


(comment
  (def data (fetch-latest-headlines))
  (filter #(clojure.string/includes? (:title %) "Πρ") (:articles data))
  (first data))

(defn db-fmt [headline]
  (let [author (util/trim-to-null (:author headline))
        published-at (clojure.instant/read-instant-date (:publishedAt headline))
        description (util/strip-html (:description headline))
        source (:source headline)
        source-name (:name source)
        title (:title headline)
        id (util/uuid-v3 title source-name published-at)]
    {:crux.db/id id
     :mext.headline/author author
     :mext.headline/title title
     :mext.headline/description description
     :mext.headline/url (:url headline)
     :mext.headline/image (:urlToImage headline)
     :mext.headline/published-at published-at
     :mext.headline/source-id (:id source)
     :mext.headline/source-name source-name}))

(defn fetch-and-persist []
  (if-let [latest (fetch-latest-headlines)]
    (let [headlines (mapv db-fmt (:articles latest))
          existing-ids (into #{} (db/get-entities (into #{} (map :crux.db/id headlines))))
          _ (timbre/debug "existing ids found" (count existing-ids))
          new-headlines (remove #(-> (:crux.db/id %) existing-ids) headlines)]
      (timbre/info "persisting" (count new-headlines) "new headlines")
      (db/insert-all new-headlines))))

#_(fetch-latest-headlines)
#_(fetch-and-persist)
