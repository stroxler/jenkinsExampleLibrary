#!/usr/bin/env groovy

import com.stroxler.TaskTracker


def call(taskName, parents, f) {
    taskTracker = new TaskTracker()
    try {
      taskTracker.waitForParents(taskName, parents)
      taskTracker.registerRunning(taskName)
      f()
      taskTracker.registerSucceeded(taskName)
    } catch (Throwable t) {
      taskTracker.registerFailed(taskName)
    }
}
