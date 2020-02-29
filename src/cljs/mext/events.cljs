(ns mext.events
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [mext.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   ))

(re-frame/reg-event-fx
 ::initialize-db
 (fn-traced [_ _]
   {:db db/default-db
    :dispatch-n [[::fetch-headlines 0]
                 [::fetch-current-user]]}))

(re-frame/reg-event-db
 ::set-active-panel
 (fn-traced [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

;; https://github.com/day8/re-frame-http-fx
(re-frame/reg-event-fx
  ::fetch-headlines
  (fn-traced [{:keys [db]} [_ page]]
    {:db   (assoc db :fetching-headlines true)
     :http-xhrio {:method          :get
                  :uri             (str "/api/headlines?page=" page)
                  :timeout         8000                                           ;; optional see API docs
                  :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                  :on-success      [::success-headlines-result]
                  :on-failure      [::failed-headlines-result]}})) ;; TODO

(re-frame/reg-event-db
  ::success-headlines-result
  (fn-traced [db [_ result]]
    (merge db result {:fetching-headlines false})))

(re-frame/reg-event-fx
  ::set-next-headline-page
  (fn-traced [{:keys [db]} _]
    (let [p (-> db :page inc)]
      {:db (assoc db :page p)
       :dispatch [::fetch-headlines p]})))

(re-frame/reg-event-fx
  ::fetch-current-user
  (fn-traced [{:keys [db]} [_ _]]
    {:db   (assoc db :fetching-current-user true)
     :http-xhrio {:method          :get
                  :uri             (str "/api/me")
                  :timeout         8000                                           ;; optional see API docs
                  :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                  :on-success      [::success-current-user-result]
                  :on-failure      [::failed-current-user-result]}}))

(re-frame/reg-event-db
  ::success-current-user-result
  (fn-traced [db [_ result]]
    (assoc db :user result :fetching-current-user false)))

(re-frame/reg-event-db
  ::failed-current-user-result
  (fn-traced [db [_ _]]
    (assoc db :user nil :fetching-current-user false)))

(re-frame/reg-event-fx
  ::set-sentiment
  (fn-traced [{:keys [db]} [_ data]]
    {:db   (assoc db :setting-sentiment data)
     :http-xhrio {:method          :post
                  :params          {:sentiment (:sentiment data)}
                  :uri             (str "/api/headlines/" (:headline-id data) "/sentiment")
                  :timeout         8000                                           ;; optional see API docs
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                  :on-success      [::success-set-sentiment-result]
                  :on-failure      [::failed-set-sentiment-result]}})) ;; TODO

(re-frame/reg-event-db
  ::success-set-sentiment-result
  (fn-traced [db [_ result]]
    (dissoc db :setting-sentiment)))
