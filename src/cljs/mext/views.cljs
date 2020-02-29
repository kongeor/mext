(ns mext.views
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    [mext.subs :as subs]
    [mext.events :as events]
   ))

;; generic

;; todo move to util
;; https://github.com/reagent-project/reagent-cookbook/tree/master/recipes/toggle-class
(defn toggle-class [a k class1 class2]
  (if (= (@a k) class1)
    (swap! a assoc k class2)
    (swap! a assoc k class1)))

(defn menu []
  (let [local-state (reagent/atom {:menu-class "navbar-menu"})
        toggle-menu #(toggle-class local-state :menu-class "navbar-menu" "navbar-menu is-active")]
    (fn []
      [:nav.navbar {:role "navigation" :aria-label "main navigation"}
       [:div.navbar-brand
        [:a.navbar-item {:href "/#/"} "mext"]
        [:a {:role "button" :class "navbar-burger burger" :aria-label "menu"
             :aria-expanded "false" :data-target "navbar-basic"
             :on-click toggle-menu}
         [:span {:aria-hidden "true"}]
         [:span {:aria-hidden "true"}]
         [:span {:aria-hidden "true"}]]]
       [:div {:id "navbar-basic"
              :class (@local-state :menu-class)}
        [:div.navbar-start
         [:a.navbar-item {:href "#/" :on-click toggle-menu} "Home"]
         [:a.navbar-item {:href "#/login" :on-click toggle-menu} "Login"]]]])))

;; home

(defn user-headline-buttons [h]
  (let [user @(re-frame/subscribe [::subs/user])]
    [:footer.card-footer
     [:a.card-footer-item {:href (str "/api/headlines/" (:id h) "/view") :target "_blank"} "view"] ;; TODO
     (when user
       [:a.card-footer-item {:on-click #(re-frame.core/dispatch [::events/set-sentiment {:headline-id (:id h)
                                                                                         :sentiment "boring"}])} "αδιάφορο"])
     (when user
       [:a.card-footer-item {:on-click #(re-frame.core/dispatch [::events/set-sentiment {:headline-id (:id h)
                                                                                         :sentiment "cool"}])} "ενδιαφέρον"])]
    ))

(defn headline [h]
  (let [user @(re-frame/subscribe [::subs/user])]
    [:div.column
     [:div.card
      [:div.card-content
       [:div.media
        [:div.media-content
         [:p.title.is-4 (:title h)]
         [:p.subtitle.is-6 (:publishedAt h)]]]
       [:div.content (:description h)]]
      [user-headline-buttons h]
      ]]))

(defn home-panel []
  (let [fetching? @(re-frame/subscribe [::subs/fetching-headlines])]
    [:div
     (let [all-headlines @(re-frame/subscribe [::subs/headlines])
           loading? @(re-frame/subscribe [::subs/fetching-headlines])]
       (if loading?
         [:div
          [:div.fa-3x
           [:i.fas.fa-circle-notch.fa-spin]]
          #_[:p "loading!"]]
         (let [headlines-sets (partition-all 4 all-headlines)]
           (for [hl-s headlines-sets]
             ^{:key hl-s} [:div.columns                     ;; TODO check
              (for [h hl-s]
                ^{:key h} [headline h])]))))
     [:input.button.is-primary {:type     "button" :value "mext!"
                                :disabled fetching?
                                :on-click #(re-frame.core/dispatch [::events/set-next-headline-page])}]]))

;; about

(defn login-panel []
  [:div
   [:h1 "Login page."]
   [:a.button.is-primary {:href "/api/login"} "login"]])

;; main

(defn- panels [panel-name]
  [:section.section
   [:div.container
    [menu]
    (case panel-name
      :home-panel [home-panel]
      :login-panel [login-panel]
      [:div])]])

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [show-panel @active-panel]))
