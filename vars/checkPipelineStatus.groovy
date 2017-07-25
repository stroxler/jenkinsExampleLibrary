#!/usr/bin/env groovy 
import com.stroxler.TaskTracker


def call() {
    TaskTracker taskTracker = new TaskTracker()
    taskTracker.propagateTaskFailures()
}
