(ns era.core
  "Cross-platform Date(Time) conversion & coercion; the API reflects the
  varied types of dates in Java, but every method also works in ClojureScript,
  but simply returns a js/Date instead of the different JVM types.

  JavaScript's single Date type seems to me to be most similar to Java's
  OffsetDateTime, so whenever a default date instance surfaces on the JVM
  side, it will be a java.time.OffsetDateTime."
  (:require [clojure.string :as str])
  #?(:clj (:import (java.util Date)
                   (java.sql Timestamp)
                   (java.time Instant OffsetDateTime ZonedDateTime ZoneId ZoneOffset))))

(defn now
  "Return the current datetime as a OffsetDateTime on the JVM,
  or a Date instance in JavaScript"
  []
  #?(:clj  (OffsetDateTime/now)
     :cljs (js/Date.)))

(defprotocol Coercions
  "Coerce between various Date types. Many of these use Instant as a lingua-franca,
  converting to Instant where needed, first, and then calling the same function again."
  (^{:tag #?(:clj OffsetDateTime :cljs js/Date)} ->OffsetDateTime [value]
    "JVM: Convert various Java date types to / parse strings as java.time.OffsetDateTime.
    CLJS: Convert to a Date.")
  (^{:tag #?(:clj ZonedDateTime :cljs js/Date)} ->ZonedDateTime [value]
    "JVM: Convert various Java date types to / parse strings as java.time.ZonedDateTime.
    CLJS: Convert to a Date.")
  (^{:tag #?(:clj Instant :cljs js/Date)} ->Instant [value]
    "JVM: Convert various Java date types to / parse strings as java.time.Instant.
    CLJS: Convert to a Date.")
  (^{:tag #?(:clj Timestamp :cljs js/Date)} ->Timestamp [value]
    "JVM: Convert various Java date types to / parse strings as java.sql.Timestamp.
    CLJS: Convert to a Date.")
  (^{:tag #?(:clj Date :cljs js/Date)} ->Date [value]
    "JVM: Convert various Java date types to / parse strings as java.util.Date.
    CLJS: Convert to a Date."))

(extend-protocol Coercions
  ;; from java.time.OffsetDateTime to ...
  #?(:clj OffsetDateTime :cljs js/Date)
  (->OffsetDateTime [this] this) ; identity
  (->ZonedDateTime [this]
    #?(:clj (.toZonedDateTime this) :cljs this))
  (->Instant [this]
    #?(:clj (.toInstant this) :cljs this))
  (->Timestamp [this]
    #?(:clj (->Timestamp (.toInstant this)) :cljs this))
  (->Date [this]
    #?(:clj (->Date (.toInstant this)) :cljs this))

  ;; from java.time.ZonedDateTime to ...
  #?@(:clj
   [ZonedDateTime
    (->OffsetDateTime [this] (.toOffsetDateTime this))
    (->ZonedDateTime  [this] this) ; identity
    (->Instant        [this] (.toInstant this))
    (->Timestamp      [this] (->Timestamp (->Instant this)))
    (->Date           [this] (->Date (->Instant this)))])

  ;; from java.time.Instant to ...
  #?@(:clj
   [Instant
    (->OffsetDateTime [this] (.atOffset this (-> (ZoneId/systemDefault) (.getRules) (.getOffset this))))
    (->ZonedDateTime  [this] (.atZone this (ZoneId/systemDefault)))
    (->Instant        [this] this) ; identity
    (->Timestamp      [this] (Timestamp/from this))
    (->Date           [this] (Date/from this))])

  ;; from java.sql.Timestamp to ...
  #?@(:clj
   [Timestamp
    (->OffsetDateTime [this] (->OffsetDateTime (.toInstant this)))
    (->ZonedDateTime  [this] (->ZonedDateTime (.toInstant this)))
    (->Instant        [this] (.toInstant this))
    (->Timestamp      [this] this) ; identity
    (->Date           [this] (Date. (.getTime this)))])

  ;; from java.util.Date to ...
  #?@(:clj
   [Date
    (->OffsetDateTime [this] (->OffsetDateTime (.toInstant this)))
    (->ZonedDateTime  [this] (->ZonedDateTime (.toInstant this)))
    (->Instant        [this] (.toInstant this))
    (->Timestamp      [this] (Timestamp. (.getTime this)))
    (->Date           [this] this)]) ; identity

  ;; from String to ...
  #?(:clj  String
     :cljs string)
  (->OffsetDateTime [this]
    #?(:clj  (OffsetDateTime/parse this)
       :cljs (js/Date. this)))
  (->ZonedDateTime [this]
    #?(:clj  (ZonedDateTime/parse this)
       :cljs (js/Date. this)))
  (->Instant [this]
    #?(:clj  (Instant/parse this)
       :cljs (js/Date. this)))
  (->Timestamp [this]
    #?(:clj  (->Timestamp (->Instant this))
       :cljs (js/Date. this)))
  (->Date [this]
    #?(:clj  (->Date (->Instant this))
       :cljs (js/Date. this)))

  ;; from nil to ... (it doesn't matter, they all return nil)
  nil
  (->OffsetDateTime [_] nil)
  (->ZonedDateTime [_] nil)
  (->Instant [_] nil)
  (->Timestamp [_] nil)
  (->Date [_] nil))

(defn inst=
  "Compare each DateTime object in `values` after converting to Instants"
  [& values]
  (apply = (map ->Instant values)))
