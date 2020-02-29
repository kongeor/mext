(defproject mext "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.danielsz/system "0.4.5"]
                 [compojure "1.6.1"]
                 [environ"1.1.0"]
                 [ring "1.8.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-middleware-format "0.7.4"]
                 [http-kit "2.4.0-alpha6"]
                 [juxt/crux-core "20.01-1.6.2-alpha"]
                 [juxt/crux-rocksdb "20.01-1.6.2-alpha"]
                 [juxt/crux-jdbc "20.01-1.6.2-alpha"]
                 [org.postgresql/postgresql "42.2.10"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/carmine "2.19.1"]
                 [clojurewerkz/quartzite "2.1.0"]
                 [javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]
                 [clj-http "3.10.0"]
                 [enlive "1.1.6"]
                 ;; cljs
                 [org.clojure/clojurescript "1.10.597"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library]]
                 [thheller/shadow-cljs "2.8.88"]
                 [reagent "0.9.1"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [re-frame "0.11.0"]
                 [secretary "1.2.3"]]

  :plugins [[lein-environ "1.0.0"]
            [lein-pprint "1.2.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "resources/public/css"]

  :aliases {"dev"        ["with-profile" "dev" "run" "-m" "shadow.cljs.devtools.cli" "watch" "app"]
            "prod"       ["with-profile" "prod" "run" "-m" "shadow.cljs.devtools.cli" "release" "app"]
            "karma-once" ["with-profile" "prod" "do"
                          ["clean"]
                          ["run" "-m" "shadow.cljs.devtools.cli" "compile" "karma-test"]
                          ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}


  :profiles {:dev [:project/dev :profiles/dev]
             :profiles/dev {}
             :project/dev {:source-paths ["dev"]
                           :dependencies [[binaryage/devtools "1.0.0"]
                                          [day8.re-frame/re-frame-10x "0.5.2"]
                                          [day8.re-frame/tracing "0.5.3"]]}
             :prod {:env          {:http-port 8000
                                   :repl-port 8001}
                    :dependencies [[org.clojure/tools.nrepl "0.2.13"]
                                   [day8.re-frame/tracing-stubs "0.5.3"]]}
             :uberjar {:source-paths ["env/prod/clj"]
                       :dependencies [[day8.re-frame/tracing-stubs "0.5.3"]]
                       :omit-source  true
                       :aot          :all
                       :uberjar-name "mext.jar"
                       :prep-tasks   ["compile" ["prod"]]}}
  :main ^:skip-aot mext.core
  :repl-options {:init-ns user}
  )
