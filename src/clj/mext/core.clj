(ns mext.core
  (:require [system.repl :refer [set-init! go]]
            [mext.systems :refer [base-system]]
            [environ.core :refer [env]]
            #_[clojure.tools.nrepl.server :as serv])
  (:gen-class))

(defn -main
  "Start a production system, unless a system is passed as argument (as in the dev-run task)."
  [& args]
  (let [system (or (first args) #'base-system)]
    #_(if-let [nrepl-port (:nrepl-port env)]
      (serv/start-server :port (Integer. nrepl-port)))
    (set-init! system)
    (go)))

