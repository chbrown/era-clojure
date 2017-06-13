## era

[![Clojars Project](https://img.shields.io/clojars/v/era.svg)](https://clojars.org/era)

Cross-platform DateTime type coercion, parsing, and manipulation.

Supports Clojure and ClojureScript via reader conditionals,
so it requires Clojure 1.7.0, `[org.clojure/clojure "1.7.0"]`, or newer.

When compiled for the Clojure/Java platform,
it uses the `java.time.*` API (a.k.a. `JSR-310`), so Java 8 is required.


### Terminology

- **Timestamp** (= **Instant**): a finite point in linear time, measurable in milliseconds since the epoch
- **Duration**: an amount of linear time, measurable in milliseconds
- **Interval**: two timestamps -- or a single timestamp plus a duration (and a single bit to say whether the timestamp is the beginning or the end)
- **Period**: a semantic (non-linear) duration, like "2 weeks".
  This is a bit confusing, since the ISO-8601 prefix/indicator for duration syntax is "P", but it is used for duration, not period -- since you can only have hour, minute, and second values in that duration.


### Alternatives

- `hara-time`
  [Documentation](http://docs.caudate.me/hara/hara-time.html)
  [Source](https://github.com/zcaudate/hara)
  > "time as a clojure map"
  JVM-only; uses Joda time for Java <8.
- `clj-time`
  [Documentation](https://clj-time.github.io/clj-time/doc/index.html)
  [Source](https://github.com/clj-time/clj-time)
  JVM-only; depends on Joda time even with Java 8.


## License

Copyright © 2017 Christopher Brown. [Eclipse Public License - v 1.0](https://www.eclipse.org/legal/epl-v10.html).
