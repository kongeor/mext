(ns mext.oauth
  (:require [environ.core :refer [env]]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [org.httpkit.client :as http])
  (:import (java.net URLEncoder)))

(defn url-encode [url]
  (URLEncoder/encode url "UTF-8"))

;; thank you
;; http://leonid.shevtsov.me/post/oauth2-is-easy/

(def oauth2-params
  {:client-id (:google-client-id env)
   :client-secret (:google-client-secret env)
   :authorize-uri  "https://accounts.google.com/o/oauth2/v2/auth"
   :redirect-uri (str (:app-host env) "/api/oauth/callback")
   :access-token-uri "https://www.googleapis.com/oauth2/v4/token"
   :scope "profile email"})

(defn authorize-uri
  ([csrf-token]
    (authorize-uri oauth2-params csrf-token))
  ([client-params csrf-token]
    (str
      (:authorize-uri client-params)
      "?response_type=code"
      "&client_id="
      (url-encode (:client-id client-params))
      "&redirect_uri="
      (url-encode (:redirect-uri client-params))
      "&scope="
      (url-encode (:scope client-params))
      "&state="
      (url-encode csrf-token))))

(defn get-authentication-response [csrf-token response-params]
  (if (= csrf-token (:state response-params))
    (let [options {:url (:access-token-uri oauth2-params)
                     :method :post
                     :form-params
                     {:code (:code response-params)
                      :grant_type "authorization_code"
                      :redirect_uri (:redirect-uri oauth2-params)}
                     :basic-auth [(:client-id oauth2-params) (:client-secret oauth2-params)]
                     }
            {:keys [status error body] :as res} @(http/request options)]
        (if (= status 200)
          (json/parse-string body true)
          (timbre/error "Couldn't get auth response" status error body)))
    nil))

(defn get-access-token [refresh-token]
  (let [options {:url (:access-token-uri oauth2-params)
                 :method :post
                 :form-params
                 {:grant_type "refresh_token"
                  :refresh_token refresh-token}
                 :basic-auth [(:client-id oauth2-params) (:client-secret oauth2-params)]}
            {:keys [status error body] :as res} @(http/request options)]
        (if (= status 200)
          (:access_token (json/parse-string body true))
          (timbre/error "Couldn't get access token" status error body))))

(defn fetch-user-email [token]
  (let [options {:url "https://people.googleapis.com/v1/people/me?personFields=emailAddresses"
                 :method :get
                 :headers {"Authorization" (str "Bearer " token)}}
            {:keys [status error body] :as res} @(http/request options)]
        (if (= status 200)
          (let [data (json/parse-string body true)
                emails (:emailAddresses data)]
            (when (> (count emails) 1)
              (timbre/warn "More than one email found" emails))
            (-> emails first :value))
          (timbre/error "Couldn't get access token" status error body))))
