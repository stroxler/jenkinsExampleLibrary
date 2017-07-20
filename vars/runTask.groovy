#!/usr/bin/env groovy

import com.stroxler.Bar


def call(taskName, parents, f) {
    bar = new Bar()
    try {
      bar.waitForParents(taskName, parents)
      bar.registerRunning(taskName)
      f()
      bar.registerSucceeded(taskName)
    } catch (Throwable t) {
      bar.registerFailed(taskName)
    }
}
