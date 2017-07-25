#!/usr/bin/env groovy 
import com.stroxler.TaskTracker


def call(taskName, f) {
    call(taskName,     [:], 3600, f)
}

def call(taskName, parents, f) {
    call(taskName, parents, 3600, f)
}

def call(taskName, parents, timeout_s, f) {
    TaskTracker taskTracker = new TaskTracker()
    try {
        taskTracker.waitForParents(taskName, parents)
        taskTracker.registerRunning(taskName)
        timeout(time: timeout_s, unit: 'SECONDS') { f() }
        taskTracker.registerSucceeded(taskName)
    } catch (Throwable t) {
        StringWriter sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        println(sw.toString())
        taskTracker.registerFailed(taskName)
    }
}




