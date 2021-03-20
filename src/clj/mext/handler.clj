(ns mext.handler
  (:require
    [compojure.route :as route]
    [compojure.core :refer [defroutes GET POST DELETE ANY]]
    [ring.util.response :refer [response resource-response content-type charset]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [taoensso.carmine.ring :refer [carmine-store]]
    [cheshire.core :as json]
    [environ.core :refer [env]]
    [mext.db :as db]
    [mext.oauth :as oauth]
    [mext.html :as html]
    [system.repl :refer [system]]
    [ring.util.response :as response]
    [taoensso.timbre :as timbre]
    [ring.middleware.reload :refer [wrap-reload]]
    [shadow.http.push-state :as push-state]))

(defn fmt-headline-json [h]
  {:id (:crux.db/id h)
   :author (:mext.headline/author h)
   :title (:mext.headline/title h)
   :description (:mext.headline/description h)
   :url (:mext.headline/url h)
   :image (:mext.headline/image h)
   :publishedAt (:mext.headline/published-at h)
   :tags (or (:mext.headline/tags h) #{})})

(defn headlines-handler [{query-params :query-params session :session :as request}]
  (let [limit (Integer. (get query-params "limit" 10))
        page (Integer. (get query-params "page" 0))
        count (db/get-headline-count)]
    (timbre/info "fetching headlines for" (:mext.user/id session))
    (->
      {:total count
       :page page
       :limit limit
       :headlines (map fmt-headline-json (db/get-headlines {:limit limit :offset (* limit page)}))}
      response
      )))

(defn view-headline-handler [{session :session params :params}]
  (if-let [headline (-> params :id keyword db/get-entity)]
    (do
      (let [uid (:mext.user/id session)
            user (db/get-entity uid)]
        (when user
          (timbre/info "user" uid "is viewing headline" (:crux.db/id headline))
          (db/set-interaction (:crux.db/id headline) (:crux.db/id user) :view 1)))
      (response/redirect (:mext.headline/url headline)))))

(defn add-headline-tag-handler [request]
  (if-let [user (-> request :session :mext.user/id)]
    (let [id (-> request :params :id keyword)
          body (-> request :body slurp (json/parse-string true))
          tag (-> body :tag keyword)]
      (timbre/info "user" user "is adding tag" tag "to headline" id)
      (db/add-headline-tag id tag)
      {:status 204})
    {:status 401}))

(def sentiment-mapping
  {"boring" 0
   "meh" 0.5
   "cool" 1})

(defn set-sentiment-handler [{session :session params :params body :body}]
  (if-let [headline (-> params :id keyword db/get-entity)]
    (do
      (let [uid (:mext.user/id session)
            user (db/get-entity uid)
            body (-> body slurp (json/parse-string true))]
        (when user
          (if-let [sentiment (-> body :sentiment sentiment-mapping)]
            (do
              (timbre/info "user" uid "is setting sentiment" sentiment "for headline" (:crux.db/id headline))
              (db/set-interaction (:crux.db/id headline) (:crux.db/id user) :sentiment sentiment)))))
      {:status 204})))

(defn delete-headline-tag-handler [request]
  (if-let [user (-> request :session :mext.user/id)]
    (let [id (-> request :params :id keyword)
          tag (-> request :params :tag keyword)]
      (timbre/info "user" user "is deleting tag" tag "from headline" id)
      (db/delete-headline-tag id tag)
      {:status 204})
    {:status 401}))

(defn me-handler [{session :session}]
  (if-let [uid (:mext.user/id session)]
    (let [user (db/get-entity uid)]
      (response {:id (:crux.db/id user)
                :email (:mext.user/email user)}))
    (response/not-found {})))

(defn- handle-oauth-callback [params]
  (let [keys (oauth/get-authentication-response "foo" params)]
    (if keys
      (let [{access_token :access_token refresh_token :refresh_token} keys]
        (if-let [email (oauth/fetch-user-email access_token)]
          (do
            (timbre/info "successful google login" email)
            (db/upsert-user email)))))))

(defn index-handler [req]
  (let [page-str (-> req :query-params (get "page"))
        page (if page-str
               (Integer/parseInt page-str)
               0)
        page' (inc page)]
    (html/index nil nil page')))

(defroutes routes
  (GET "/" []  index-handler)
  (GET "/api/headlines" [] headlines-handler)
  (GET "/api/headlines/:id/view" [] view-headline-handler)
  (POST "/api/headlines/:id/sentiment" [] set-sentiment-handler)
  (POST "/api/headlines/:id/tags" [] add-headline-tag-handler)
  (DELETE "/api/headlines/:id/tags/:tag" [] delete-headline-tag-handler)
  (GET "/api/me" [] me-handler)
  (GET "/api/login" []
       (fn [{session :session}]
         (response/redirect (oauth/authorize-uri "foo"))))
  (GET "/api/oauth/callback" []
       (fn [{params :params session :session}]
         (let [user (handle-oauth-callback params)
               session (assoc session :mext.user/id (:crux.db/id user))]
           (->
             (response/redirect "/")
             (assoc :session session)))))
  (route/not-found "404"))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (timbre/fatal e)
           {:status 500
            :body "Oh no! :'("}))))

#_(defn wrap-dir-index [handler]
  (fn [req]
    (handler
      (update
        req
        :uri
        #(if (= "/" %) "/index.html" %)))))

(def app
  (-> routes
    #_(wrap-restful-format :formats [:json])
    (wrap-defaults (-> site-defaults
                     (assoc-in [:session :store] (carmine-store {:pool {} :spec {:uri (:redis-url env)}} {:key-prefix "mext"})) ;; TODO secure
                     (assoc-in [:session :cookie-attrs :max-age] (* 60 60 24 30)) ;; month
                     (assoc-in [:security :anti-forgery] false))) ;; TODO check too
    ; wrap-dir-index
    wrap-exception))

;; TODO fix response
(def dev-handler (-> #'routes wrap-reload push-state/handle))
