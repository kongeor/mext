(ns mext.news-test
  (:require [clojure.test :refer :all]
            [mext.news :as news]))

(def sample
  {:source {:id "google-news", :name "Google News"},
   :author "ProtoThema.gr",
   :title "Η Έλενα Ράπτη παντρεύτηκε - Πρώτο ΘΕΜΑ",
   :description "<span>Έκπληξη η ανακοίνωση του γάμου της της βουλευτού της ΝΔ, που έκανε γνωστό το ευτυχές γεγονός  μέσω Instagram – Ο γάμος έγινε πριν λίγο καιρό σε πολύ στενό οικογενειακό κύκλο</span>",
   :url "https://news.google.com/__i/rss/rd/articles/CBMiTWh0dHBzOi8vd3d3LnByb3RvdGhlbWEuZ3IvbGlmZS1zdHlsZS9hcnRpY2xlLzkxNzE4Mi9pLWVsZW5hLXJhcHRpLXBhZHJldXRpa2Uv0gEA?oc=5",
   :urlToImage "https://i1.prth.gr/images/640x360share/files/2019-08-18/elenarapti.jpg",
   :publishedAt "2019-08-18T09:55:00Z",
   :content nil})

(def db-sample
  {:crux.db/id :7d251a03545de01ba0d87a80d8b7e2e8,
              :mext.headline/author "ProtoThema.gr",
              :mext.headline/title "Η Έλενα Ράπτη παντρεύτηκε - Πρώτο ΘΕΜΑ",
              :mext.headline/description "Έκπληξη η ανακοίνωση του γάμου της της βουλευτού της ΝΔ, που έκανε γνωστό το ευτυχές γεγονός  μέσω Instagram – Ο γάμος έγινε πριν λίγο καιρό σε πολύ στενό οικογενειακό κύκλο",
              :mext.headline/url "https://news.google.com/__i/rss/rd/articles/CBMiTWh0dHBzOi8vd3d3LnByb3RvdGhlbWEuZ3IvbGlmZS1zdHlsZS9hcnRpY2xlLzkxNzE4Mi9pLWVsZW5hLXJhcHRpLXBhZHJldXRpa2Uv0gEA?oc=5",
              :mext.headline/image "https://i1.prth.gr/images/640x360share/files/2019-08-18/elenarapti.jpg",
              :mext.headline/published-at #inst "2019-08-18T09:55:00.000-00:00"})

(def unsupported-db-sample
  {:crux.db/id :99b71def3937f5dcdcd8a0dc713046d2,
   :mext.headline/author nil,
   :mext.headline/title "Molde Εναντίον Άρης LIVE | Live Ποδόσφαιρο - UEFA Europa League Qualification | 08-08-19 | gazzetta.gr - gazzetta.gr",
   :mext.headline/description "Molde Εναντίον Άρης: Παρακολουθήστε live την αναμέτρηση  και σχολιάστε τον αγώνα στο chat του gazzetta.",
   :mext.headline/url "https://news.google.com/__i/rss/rd/articles/CBMiLmh0dHA6Ly93d3cuZ2F6emV0dGEuZ3IvZ2FtZS1zdGF0cy8xODE1LzMxMzc3MTXSAQA?oc=5",
   :mext.headline/image "http://www.gazzetta.gr/sites/default/files/styles/ogimage_watermark/public/article/2019-08/2182777.jpg?itok=zCj52TBv",
   :mext.headline/published-at #inst"2019-08-08T19:00:00.000-00:00"})

;; fails on CI
(deftest formatting-headlines
  (testing "stuff"
    (is (= {:crux.db/id #uuid "512b555f-d52f-3719-b602-a6ffdbca126c",
            :mext.headline/author "ProtoThema.gr",
            :mext.headline/title "Η Έλενα Ράπτη παντρεύτηκε - Πρώτο ΘΕΜΑ",
            :mext.headline/description "Έκπληξη η ανακοίνωση του γάμου της της βουλευτού της ΝΔ, που έκανε γνωστό το ευτυχές γεγονός  μέσω Instagram – Ο γάμος έγινε πριν λίγο καιρό σε πολύ στενό οικογενειακό κύκλο",
            :mext.headline/url "https://news.google.com/__i/rss/rd/articles/CBMiTWh0dHBzOi8vd3d3LnByb3RvdGhlbWEuZ3IvbGlmZS1zdHlsZS9hcnRpY2xlLzkxNzE4Mi9pLWVsZW5hLXJhcHRpLXBhZHJldXRpa2Uv0gEA?oc=5",
            :mext.headline/image "https://i1.prth.gr/images/640x360share/files/2019-08-18/elenarapti.jpg",
            :mext.headline/published-at #inst "2019-08-18T09:55:00.000-00:00"
            :mext.headline/source-id "google-news"
            :mext.headline/source-name "Google News"
            :mext.headline/search-text "ελενα ραπτη παντρευτηκε πρωτο θεμα εκπληξη ανακοινωση γαμου βουλευτου νδ εκανε γνωστο ευτυχεσ γεγονοσ μεσω instagram γαμοσ εγινε καιρο στενο οικογενειακο κυκλο"
            :mext.headline/search-text-stemmed "ελεν ραπτ παντρευτ πρωτ θεμ εκπληξ ανακοινωσ γαμ βουλευτ νδ εκ γνωστ ευτυχ γεγον μεσ instagram γαμ εγιν καιρ στεν οικογενειακ κυκλ"}
           (news/db-fmt sample)))))

