#!/usr/bin/env groovy 
import com.stroxler.TaskTracker


def call(Map... taskRuns) {
    call(taskRuns.toList())
}


def call(List<Map> taskRuns) {
    TaskTracker taskTracker = new TaskTracker()

    Map runMap = taskTracker.validateAndTransformTaskRuns(taskRuns)

    stage("dag") {
        parallel(runMap)
    }

    stage("check task statuses") {
       println(taskTracker.reportAndSetBuildStatus(currentBuild))
    }
}


