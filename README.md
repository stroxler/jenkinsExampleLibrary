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
    stage("parallel") {
        parallel([
            "block a": {
                runTask "a-1st", [], {
                    sh "sleep 5"
                    echo "hello from a-1st"
                }
                runTask "a-2nd", [], {
                    sh "sleep 5"
                    echo "hello from a-2nd"
                }
                runTask "a-3rd", [], {
                    sh "sleep 5"
                    echo "hello from a-3rd"
                }
            },
            "block b": {
                runTask "b-1st", ["a-1st"], {
                    echo "hello from b-1st"
                }
                runTask "b-2nd", ["a-3rd"], {
                    echo "hello from b-2nd"
                }
            },
            "block c": {
                runTask "c-1st", [], {
                    echo "hello from c-1st"
                }
                runTask "c-2nd", ["a-2nd"], {
                    echo "hello from c-2nd"
                }
            },
        ])
    }
}
```

Then I ran both pipelines at about the same time.

The test verifies that we are able to run two pipelines at once and
get the desired order of execution in both copies:
  - a1st and c1st start together
  - c1st finishes first
  - a1st finishes, then b1st starts and finishes while a2nd starts
  - a2nd finishes and c2nd starts and finishes while a3rd starts
  - a3rd starts and then b2nd runs
