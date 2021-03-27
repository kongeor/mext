(ns mext.util
  (:require [net.cgrand.enlive-html :as enlive])
  (:import (java.security MessageDigest)
           (com.github.kongeor.elst Elst)))

(defn uuid-v3 [& args]
  (let [s (clojure.string/join (map str args))]
    (java.util.UUID/nameUUIDFromBytes (.getBytes s))))

(defn md5 [& args]
  (let [s (clojure.string/join (map str args))
        algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn uuid [] (java.util.UUID/randomUUID))

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

(defn substring [str n]
  (if str
    (if (> (count str) n)
      (.substring str 0 n)
      str)))

(defn el-lower-and-stem [s]
  (clojure.string/join " " (Elst/lowerAndStopPhrase s)))

(defn el-lower-stop-and-stem [s]
  (clojure.string/join " " (Elst/lowerStopAndStemPhrase s)))