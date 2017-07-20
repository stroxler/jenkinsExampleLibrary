#!/usr/bin/env groovy
package com.stroxler

import java.lang.Thread
import java.util.Map
import java.util.HashMap


class Globals {
  public static Integer lock = 0
  public static Map<String, String> taskStatuses = new HashMap<>()


  @NonCPS
  public static void updateStatus(String taskName, String status) {
      if (status != "running" &&
          status != "succeeded" &&
          status != "failed") {
          throw new Exception("Status \"" + status + "\" not legal")
      }
      synchronized(lock) {
          taskStatuses.put(taskName, status)
      }
  }

  public static String getStatus(String taskName) {
      return taskStatuses.get(taskName)
  }
}


@NonCPS
def runTask(taskName, taskCode) {
    Globals.updateStatus(taskName, "running")
    try {
        out = taskCode()
        Globals.updateStatus(taskName, "succeeded")
        return out
    } catch (Throwable t) {
        Globals.updateStatus(taskName, "failed")
        throw t
    }
}
