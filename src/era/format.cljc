(ns era.format
  (:require [era.core :as core])
  #?(:clj (:import (java.time Instant OffsetDateTime ZonedDateTime ZoneId) ; required for some reason
                   (java.time.format DateTimeFormatter FormatStyle))))

(defn iso
  "Convert value to a Java(Script) date and format as a standard ISO-8601 string in UTC."
  [value]
  ; maybe return unchanged if String?
  (when value
    #?(:clj  (.toString (core/->Instant value))
       :cljs (.toISOString (core/->OffsetDateTime value)))))

(def ^:private format-styles
  "CLJS separates date and time with a comma; JVM does not.
  Otherwise these should produce the same strings."
  {; FULL -> "Monday, June 12, 2017 4:30:05 PM CDT"
   :full #?(:clj  FormatStyle/FULL
            :cljs {:weekday      "long"
                   :year         "numeric"
                   :month        "long"
                   :day          "numeric"
                   :hour         "numeric"
                   :minute       "numeric"
                   :second       "numeric"
                   :timeZoneName "short"})
   ; LONG -> "December 8, 2016 5:18:19 PM CST"
   :long #?(:clj  FormatStyle/LONG
            :cljs {:year         "numeric"
                   :month        "long"
                   :day          "numeric"
                   :hour         "numeric"
                   :minute       "numeric"
                   :second       "numeric"
                   :timeZoneName "short"})
   ; MEDIUM -> "Dec 8, 2016 5:17:42 PM"
   :medium #?(:clj  FormatStyle/MEDIUM
              :cljs {:year   "numeric"
                     :month  "short"
                     :day    "numeric"
                     :hour   "numeric"
                     :minute "numeric"
                     :second "numeric"})
   ; SHORT -> "6/12/17 4:29 PM"
   :short #?(:clj  FormatStyle/SHORT
             :cljs {:year   "2-digit"
                    :month  "numeric"
                    :day    "numeric"
                    :hour   "numeric"
                    :minute "numeric"})})

(defn locale
  "Convert `value` to an OffsetDateTime or Date and format using the current locale"
  ([value] (locale value :medium))
  ([value style]
   {:pre [(contains? format-styles style)]}
   (when-let [date (core/->OffsetDateTime value)]
     (let [format-style (get format-styles style)]
       #?(:clj (-> (DateTimeFormatter/ofLocalizedDateTime format-style)
                   (.withZone (ZoneId/systemDefault))
                   (.format date))
          :cljs (.toLocaleString date "en-US" (clj->js format-style)))))))
