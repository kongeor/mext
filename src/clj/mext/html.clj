(ns mext.html
  (:require
    [hiccup [page :refer [html5 include-js include-css]]]
    [environ.core :refer [env]]
    [mext.db :as db]
    #_[clojure.contrib.humanize :as hmn])
  (:import (java.util Date)))


(defn base [uid content]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:meta {:name "description" :content "mext"}]
     [:meta {:name "author" :content "Kostas Georgiadis"}]
     [:title "mext"]

     [:script {:src "//use.fontawesome.com/releases/v5.3.1/js/all.js"
               :defer true
               }]

     [:script {:src "//cdn.jsdelivr.net/gh/alpinejs/alpine@v2.8.1/dist/alpine.min.js"
               :defer true
               }]

     (include-css
       "//cdn.jsdelivr.net/npm/bulma@0.9.1/css/bulma.min.css")]
    [:body
     [:div.container
      [:nav.navbar.mb-4 {:role "navigation" :aria-label "main navigation"}
       [:div.navbar-brand
        [:a.navbar-item {:href (:app-host env)}
         [:h1.title "mext"]]]
       #_[:div.navbar-menu
        [:div.navbar-start
         [:div.navbar-item
          [:a.button.is-light {:href "/stats"} "Stats"]]]]
       [:div.navbar-end
        [:div.navbar-item
         [:div.buttons
          (if-not uid
            [:a.button.is-primary {:href "/login"}
             [:strong "Login"]]
            [:a.button.is-light {:href "/admin"} "Admin"]
            #_[:a.button.is-light {:href "/logout"} "Logout"])]]]]

      [:div.mb-4
       content]
      [:footer.footer
       [:div.content.has-text-centered
        #_[:p (str "version " (util/project-version))]
        #_[:p (str "total users: " (count (db/get-users)))]]]]]))

(defn headline [h]
  [:div.column
   [:div.card
    [:div.card-content
     [:div.media
      [:div.media-content
       [:p.title.is-4 (:mext.headline/title h)]
       [:p.subtitle.is-6 (:mext.headline/published-at h)]]]
     [:div.content (:mext.headline/description h)]]
    [:footer.card-footer
     [:a.card-footer-item {:href (:mext.headline/url h)} "link"]]]])

(defn index [db uid page]
  (let [headlines (db/get-headlines {:offset (* page 12) :limit 12})
        now (inst-ms (Date.))]
    (base
      uid
      [:div
       (for [headline-data (partition-all 4 headlines)]
         [:div.columns
          (for [h headline-data]
            (headline h))])
       [:div.columns.is-centered
        [:a.button.is-primary {:href (str "/?page=" page)} "Load moar"]]])))

(defn tag-form [db uid page]
  (let [tags [{:key "tag2" :raw-value "foo bar baz"} {:key "tag2" :raw-value "quux qu"}]
        now (inst-ms (Date.))]
    (base
      uid
      [:form {:action "/admin/tags" :method "post"}
       (for [t tags]
         [:div
          [:div {:class "field is-horizontal"}
           [:div {:class "field-label is-normal"}
            [:label {:class "label"} "Tag"]]
           [:div {:class "field-body"}
            [:div {:class "field"}
             [:div {:class "control"}
              [:input.input {:name (:key t) :type "text" :value (:key t)}]]
             [:p {:class "help is-danger"} "This field is required"]]]]
          [:div {:class "field is-horizontal"}
           [:div {:class "field-label is-normal"}
            [:label {:class "label"} "Values"]]
           [:div {:class "field-body"}
            [:div {:class "field"}
             [:div {:class "control"}
              [:textarea.textarea {:name (str (:key t) "-raw-value")} (:raw-value t)]]]]]
          [:hr]])
       [:div {:class "field is-horizontal"}
        [:div {:class "field-label"} "<!-- Left empty for spacing -->"]
        [:div {:class "field-body"}
         [:div {:class "field"}
          [:div {:class "control"}
           [:input.button.is-primary {:type "submit" :value "Send message"}]]]]]])))


(defn tag-form-test [db uid page]
  (let [data {:open false :items [{:key "tag1" :raw-value "foo bar baz"} {:key "tag2" :raw-value "quux qu!!"}]}
        now (inst-ms (Date.))]
    (base
      uid

      [:div
       [:div {:x-data (clojure.data.json/write-str data)}
        [:button {"@click" "open = true"} "show"]
        [:ul {:x-show "open" "@click.away" "open = false"} "dropdown"]
        [:template {:x-show "items" :x-for "(item, index) in items" ":key" "item"}
         [:div
          [:div {:x-text "item.key"}]
          [:div {:x-text "item['raw-value']"}]
          [:button {"@click" "items.splice(index, 1)"} "x"]]
         ]]]
      )))

(defn login [db]
  (let []
    (base
      nil
      [:form {:action "/login" :method "post"}
       [:div.field
        [:p.control.has-icons-left.has-icons-right
         [:input.input {:type "text" :placeholder "username" :name "username"}]
         [:span.icon.is-small.is-left
          [:i.fas.fa-envelope]]
         [:span.icon.is-small.is-right
          [:i.fas.fa-check]]]]
       [:div.field
        [:p.control.has-icons-left
         [:input.input {:type "password" :placeholder "Password" :name "password"}]
         [:span.icon.is-small.is-left
          [:i.fas.fa-lock]]]]
       [:div.field
        [:p.control
         [:button.button.is-success "Login"]]]])))

(defn admin [_db uid]
  (let [user (db/get-user-by-id uid)
        blacklist (:mext.user/blacklist user)]
    (base
      uid
      [:form {:action "/admin/blacklist" :method "post"}
       [:div.field
        [:p.control.has-icons-left.has-icons-right
         [:input.input {:type "text" :placeholder "blacklist" :name "blacklist" :value blacklist}]
         [:span.icon.is-small.is-left
          [:i.fas.fa-envelope]]
         [:span.icon.is-small.is-right
          [:i.fas.fa-check]]]]
       [:div.field
        [:p.control
         [:button.button.is-success "Save"]]]])))
