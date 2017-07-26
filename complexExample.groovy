@Library("jenkinsExampleLibrary") _

/*
 * This example job should fail. The expected status at the end is:

Task status report
-----------------

Succeeded:
  [everything-finished, does-run]

Failed:
  [task-a]

Skipped:
  [does-not-run]
 */

node {
    dag(
        task("task-a") {
            timeout(time: 3, unit: 'SECONDS') {
              sh "sleep 10"
              echo "a"
            }
        },
        task("does-not-run", ["task-a"]) {
            echo "oops, expected task a to fail"
        },
        task("does-run", ["failed": ["task-a"]]) {
            echo "as expected, task-a failed"
        },
        task("everything-finished",
            ["succeeded": ["does-run"],
             "completed": ["task-a", "does-not-run"]]) {
             echo "everything before this task has run"
        },
    )
}
