@Library("jenkinsExampleLibrary") _

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
        }
    )
}
