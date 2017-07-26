@Library("jenkinsExampleLibrary") _


/*
 * This example job should fail. The expected status at the end is:

Task status report
-----------------

Succeeded:
  [task-a, task-b]

Failed:
  []

Skipped:
  [does-not-run]
 */

node {
    dag(
        task("task-a") {
            sh "sleep 10"
            echo "a"
        },
        task("task-b", ["task-a"]) {
            echo "b"
        },
        task("does-not-run", ["failed": ["task-b"]]) {
            echo "oops, task b failed"
        },
    )
}
