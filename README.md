# Pure Scala functional programming using ZIO

## Hacker News API

A simple implementation of the Hacker News API

I use uPickle for json parsing and the scalaj-http library for interacting with
the API.

## Performance

ScalaJ-Http is a blocking library. When doing lots of requests we could deplete
the main thread pool, so I use ZIO's blocking thread pool.

## Testing

Check out the test suite for how we switch the real HttpClient for a mock one





