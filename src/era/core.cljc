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
                   (java.time Instant OffsetDateTime ZonedDateTime ZoneId ZoneOffset)
                   (java.time.temporal ChronoUnit TemporalAmount TemporalUnit))))

(defn now
  "Return the current datetime as a OffsetDateTime on the JVM,
  or a Date instance in JavaScript"
  []
  #?(:clj  (OffsetDateTime/now)
     :cljs (js/Date.)))

(def ^:private offset-re #"([-+])(\d\d)(?::?(\d\d))?$")

(defn- parse-string
  "JVM: Parse `s` as a java.time.OffsetDateTime.
  CLJS: Ensure that `s` is a string that can be parsed by the
  built-in Date string parser, according to the spec:
  https://www.ecma-international.org/ecma-262/5.1/#sec-15.9.4.2
  to ensure cross-browser consistency, and return a js/Date."
  [value]
  #?(:clj  (OffsetDateTime/parse value)
     :cljs (let [; remove the timezone declaration, if suffixed
                 ; TODO: interpret the timezone, since it takes precedence
                 ;       over the offset, if provided, in java.time
                 s-with-offset (str/replace-first value #"\[[^\[\]]+\]$" "")
                 ; replace the ±hh:mm offset with Z, if suffixed,
                 ; in which case we re-find that offset and use it later
                 s (str/replace-first s-with-offset offset-re "Z")
                 millis (js/Date.parse s)]
             (if-let [[_ sign hh mm] (re-find offset-re s-with-offset)]
               (let [multiplier (if (= sign "+") -1 1)
                     offset-millis (* multiplier
                                      (+ (* 3600000 (js/parseInt hh 10))
                                         (* 60000 (js/parseInt (or mm "00") 10))))]
                 (js/Date. (+ millis offset-millis)))
               (js/Date. millis)))))

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
    (parse-string this))
  (->ZonedDateTime [this]
    #?(:clj  (ZonedDateTime/parse this)
       :cljs (parse-string this)))
  (->Instant [this]
    #?(:clj  (Instant/parse this)
       :cljs (parse-string this)))
  (->Timestamp [this]
    #?(:clj  (->Timestamp (->Instant this))
       :cljs (parse-string this)))
  (->Date [this]
    #?(:clj  (->Date (->Instant this))
       :cljs (parse-string this)))

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

(defprotocol Temporal
  "Similar to the java.time.Temporal interface, this protocol defines a
  manipulation on DateTime instances."
  (add [datetime duration]
    "Add `duration` to `datetime`.
    The `duration` can be a number, signifying seconds (which is the SI unit for time),
    or a sequence of 2-tuples of [duration-unit-type, numeric-value], e.g., a map,
    with the following valid duration unit types:
      :nanos :micros :millis :seconds :minutes :hours :half-days
      :days :weeks :months :years :decades :centuries :millennia :eras :forever
    These names are based on the java.time.temporal.ChronoUnit enum.
    Each duration component is added left-to-right, and sometimes order matters;
    e.g., (2001-02-28 + 1 day) + 1 month => 2001-04-01 ≠
          (2001-02-28 + 1 month) + 1 day => 2001-03-29
    On the JVM, `duration` can also be a TemporalAmount instance."))

(def ^:private millis-per-unit
  {:nanos                #?(:clj 1/1000000 :cljs 0.000001)
   :micros               #?(:clj 1/1000    :cljs 0.001)
   :millis                       1
   :seconds                   1000
   :minutes                  60000
   :hours                  3600000
   :half-days             43200000
   :days                  86400000
   :weeks                604800000
   :months              2629746000 ; approximation using years from below / 12
   :years              31556952000 ; approximation using average days / year: 365 + 1/4 - 1/100 + 1/400 = 365.2425
   :decades           315569520000
   :centuries        3155695200000
   :millennia       31556952000000
   :eras      31556952000000000000
   :forever   #?(:clj Double/POSITIVE_INFINITY :cljs js/Infinity)})

#?(:clj
  (def duration-unit-type->TemporalUnit
    {:nanos     ChronoUnit/NANOS
     :micros    ChronoUnit/MICROS
     :millis    ChronoUnit/MILLIS
     :seconds   ChronoUnit/SECONDS
     :minutes   ChronoUnit/MINUTES
     :hours     ChronoUnit/HOURS
     :half-days ChronoUnit/HALF_DAYS
     :days      ChronoUnit/DAYS
     :weeks     ChronoUnit/WEEKS
     :months    ChronoUnit/MONTHS
     :years     ChronoUnit/YEARS
     :decades   ChronoUnit/DECADES
     :centuries ChronoUnit/CENTURIES
     :millennia ChronoUnit/MILLENNIA
     :eras      ChronoUnit/ERAS
     :forever   ChronoUnit/FOREVER}))


(extend-protocol Temporal
  #?(:clj java.time.temporal.Temporal :cljs js/Date)
  (add [this duration]
    #?(:clj
      (if (instance? TemporalAmount duration)
        ; handle JVM special case where duration is a java.time.temporal.TemporalAmount
        (.addTo ^TemporalAmount duration this)
        ; if duration isn't a map assume it's numeric seconds
        (let [duration (if (map? duration) duration {:seconds duration})]
          (reduce (fn [date [type units]]
                    (let [temporal-unit (get duration-unit-type->TemporalUnit type type)]
                      (.addTo ^TemporalUnit temporal-unit date units))) this duration)))
      :cljs
      (let [duration (if (map? duration) duration {:seconds duration})]
        (reduce (fn [date [type units]]
                  (let [millis (* (get millis-per-unit type) units)]
                    (js/Date. (+ (.getTime date) millis)))) this duration)))))
