(ns mext.systems
  (:require [system.core :refer [defsystem]]
            (system.components
              [http-kit :refer [new-web-server]])
            [mext.handler :refer [app]]
            [mext.news :as news]
            [mext.loggly :as loggly]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [system.repl :refer [system]]
            [crux.api :as crux]
            [crux.jdbc]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.simple :refer [schedule repeat-forever with-interval-in-minutes]]
            [clojurewerkz.quartzite.jobs :refer [defjob] :as j]
            [clojure.java.io :as io])
  )

(if-let [token (env :loggly-api-token)]
  (if-not (clojure.string/blank? token)
    (timbre/merge-config!
      {:appenders
       {:loggly (loggly/loggly-appender
                  {:tags [:mext]
                   :token token})}})))

;; crux

(defrecord CruxDb [db]
  component/Lifecycle
  (start [component]
    (let [db (crux/start-node
               {:crux.jdbc/connection-pool {:dialect {:crux/module 'crux.jdbc.psql/->dialect}
                                            ; :pool-opts { ... }
                                            :db-spec {:dbname (env :pg-db)
                                                      :host (env :pg-host)
                                                      :user (env :pg-user)
                                                      :password (env :pg-pass)}
                                                      }
                :crux/tx-log {:crux/module 'crux.jdbc/->tx-log
                              :connection-pool :crux.jdbc/connection-pool}
                :crux/document-store {:crux/module 'crux.jdbc/->document-store
                                      :connection-pool :crux.jdbc/connection-pool}
                :crux/index-store {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                                              :db-dir (io/file "data")}}
                :crux.lucene/lucene-store {:db-dir "lucene-dir" :indexer 'crux.lucene.multi-field/->indexer}})]
      (timbre/info "starting crux")
      (assoc component :db db)))
  (stop [component]
    (when db
      (timbre/info "stopping crux")
      (.close db)
      component)))

(defn- new-db []
  (map->CruxDb nil))

;; quartzite

(defjob HistoryWatcher
        [ctx]
        (news/fetch-and-persist))

(defn str->int [s]
  (Integer/parseInt s))

(defn schedule-history-watcher [scheduler]
  (let [job (j/build
              (j/of-type HistoryWatcher)
              (j/with-identity (j/key "jobs.history.1")))
        trigger (t/build
                  (t/with-identity (t/key "triggers.1"))
                  (t/start-now)
                  (t/with-schedule (schedule
                                     (repeat-forever)
                                     (with-interval-in-minutes (or (-> env :news-watcher-interval str->int) 15)))))]
    (qs/schedule scheduler job trigger)))

(defrecord Scheduler [scheduler]
  component/Lifecycle
  (start [component]
    (let [s (-> (qs/initialize) qs/start)]
      (schedule-history-watcher s)                          ;; TODO ask
      (assoc component :scheduler s)))
  (stop [component]
    (qs/shutdown scheduler)
    component))

(defn new-scheduler
  []
  (map->Scheduler {}))

(defsystem base-system
           [:db (new-db)
            :web (new-web-server (Integer. (env :http-port)) app)
            :scheduler (new-scheduler)])