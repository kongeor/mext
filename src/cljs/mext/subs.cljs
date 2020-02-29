(ns mext.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
  ::headlines
  (fn [db _]
    (:headlines db)))

(re-frame/reg-sub
  ::fetching-headlines
  (fn [db _]
    (:fetching-headlines db)))

(re-frame/reg-sub
  ::user
  (fn [db _]
    (:user db)))

;; TODO create generic reg
