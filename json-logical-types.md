# JSON logical types

`Event.toJson()` produces a human-readable JSON view, not Avro's wire JSON encoding.
JSON has no native date/time type; ISO 8601 strings are the default here, with RFC 3339-style
UTC timestamps (`Z`). The Avro logical types are rendered as follows:

| Logical type | JSON representation |
|---|---|
| `uuid` | UUID string |
| `date` | `"2024-02-29"` |
| `time-millis`, `time-micros` | `"12:34:56.123"`, `"12:34:56.123456"` |
| `timestamp-millis`, `timestamp-micros` | `"2024-02-29T12:34:56.123Z"`, `"2024-02-29T12:34:56.123456Z"` |
| `local-timestamp-millis`, `local-timestamp-micros` | `"2024-02-29T12:34:56.123"`, `"2024-02-29T12:34:56.123456"` (no timezone) |
| `duration` | `{"months":1,"days":2,"milliseconds":3}` |
| `decimal` | JSON number |

Fractional seconds retain the logical type's precision; trailing zeroes may be omitted.
Durations retain their three unsigned components rather than approximating calendar months as elapsed time.
Non-null unions retain their Avro type-name wrapper, e.g. `{"int":"2024-02-29"}` for a nullable date.
Previously numeric temporal logical types now produce strings; raw numeric fields without logical types are unchanged.

Override formats per call using `EventJsonFormat` and `java.time.format.DateTimeFormatter`:

```kotlin
event.toJson(
  EventJsonFormat(
    date = DateTimeFormatter.ofPattern("dd/MM/uuuu"),
    timestamp = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS XXX"),
  ),
)
```

The other options are `time` and `localTimestamp`. Formats apply recursively to nested records,
unions, arrays and maps. Timestamp formatters always use UTC; local timestamps never acquire a timezone.
Incompatible formatters throw an exception instead of silently falling back. This per-call option does not
change the default format used by the event history API.
