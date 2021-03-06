(ns mext.util
  (:require [net.cgrand.enlive-html :as enlive])
  (:import (java.security MessageDigest)))

(defn md5 [& args]
  (let [s (clojure.string/join (map str args))
        algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defn manifest-map
  "Returns the mainAttributes of the manifest of the passed in class as a map."
  [clazz]
  (->> (str "jar:"
            (-> clazz
              .getProtectionDomain
              .getCodeSource
              .getLocation)
            "!/META-INF/MANIFEST.MF")
       clojure.java.io/input-stream
       java.util.jar.Manifest.
       .getMainAttributes
       (map (fn [[k v]] [(str k) v]))
       (into {})))

(defn project-version []
  (if-let [version (System/getProperty "mext.version")]
    version
    (get (manifest-map (Class/forName "mext.core")) "Leiningen-Project-Version")))

;; string utils

(defn trim-to-null [^String str]
  (if str
    (if-let [s (clojure.string/trim str)]
      (if (> (count s) 0)
        s))))

(defn strip-html [str]
  (clojure.string/join "" (map enlive/text (->> (enlive/html-snippet str)))))

(defn word-count [str]
  (if str
    (count (clojure.string/split str #" "))))
