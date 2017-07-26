#!/usr/bin/env groovy 
import com.stroxler.DagRunner


def call(Map... taskRuns) {
    call(taskRuns.toList())
}


def call(List<Map> taskRuns) {
    DagRunner dr = new DagRunner()

    Map runMap = dr.validateAndTransformTaskRuns(taskRuns)

    stage("dag") {
        parallel(runMap)
    }

    stage("check task statuses") {
       println(dr.reportAndSetBuildStatus(currentBuild))
    }
}


