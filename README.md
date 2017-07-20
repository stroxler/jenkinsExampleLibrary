# Example of a groovy shared library for jenkins

I'm interested in how to write custom jenkins functionality that includes
(isolated) state managment within the execution of a pipeline, with an
eye toward allowing airflow-style DAGs of tasks inside jenkins pipelines.

This plugin is a first step toward exploring this possiblility.

Here's how I tested it out: I configured my jenkins development instance
to use this plugin, and then made two copies of a job using this groovy
script:
```groovy
@Library("jenkinsExampleLibrary") _

node {
    stage("say hello from library") {
        squareTwo()
        squareTwo()
        squareTwo()
    }
}
```

Then I ran both pipelines at about the same time.

Since the code inside `Bar` called via `squareTwo` both sleeps for 10 seconds
and modifies a static variable inside `Bar.Globals`, I was able to verify that
  - the global state in `Bar.Globals` is persisted across calls to
    `squareTwo` within a single pipeline (it's not obvious that this would
    happen; `Globals` is not a static class, and the `Bar` instance is fresh
    every time, so it wouldn't have been surprising to get a different
    `Globals` in each call)
  - the global state does *not* appear to be shared between the two
    simultaneously running pipelines. I'm fuzzy on details about how the
    pipeline plugin runs things, but it seems like it might be spinning up
    a fresh jvm, or isolating different runs in some other way.

As a result, it's probably possible to provide an implementation of
an action that declares parents, and use plain old groovy / java code to
handle thread-safe access to a hash set or hash map that tracks task state,
in order to get a lightweight DAG runner.
