#!/usr/bin/env groovy
package com.stroxler

import java.util.concurrent.ConcurrentHashMap
import java.util.HashMap
import java.util.Map
import java.util.List



@groovy.transform.Field def DEBUG
DEBUG = false
def setDEBUG(debug) {
    this.DEBUG = debug
}


def registerRunning(taskName) {
    TaskStatus.updateStatus(taskName, "running")
}


def registerSucceeded(taskName) {
    TaskStatus.updateStatus(taskName, "succeeded")
}


def registerFailed(taskName) {
    TaskStatus.updateStatus(taskName, "failed")
}


def propagateTaskFailures() {
    boolean failed = false
    String failures = ""
    for (entry in TaskStatus.taskStatuses.entrySet()) {
        String taskName = entry.getKey()
        String status = entry.getValue()
        if (status != 'succeeded') {
           failed = true
           failures += "\nTask ${taskName} had non-successful status ${status}"
        }
    }
    if (failed) {
        throw new Exception("Pipeline had non-successful tasks:" + failures)
    }
}


def waitForParents(taskName, parents) {
    while(!parentsReady(taskName, parents)) {
        sleep(1)
    }
}

def parentsReady(String taskName, List<String> parents) {
    Map<String, String> parentMap = parents.collectEntries {
        parent -> [parent, true]
    }
    parentsReady(taskName, parentMap)
}


def parentsReady(String taskName, Map<String, Boolean> parents) {
    debug("checking parents ${parents} of task ${taskName}, " +
          "current statuses: ${TaskStatus.taskStatuses}")
    boolean ready = areParentsReady(parents)
    debug("are parents ${parents} of task ${taskName} are ready? ${ready}")
    return ready
}


// internals, not part of the public api

boolean areParentsReady(Map<String, Boolean> parents) {
    for (String parent : parents.keySet()) {
        String status = TaskStatus.getStatus(parent);
        if (status == "failed") {
            boolean successRequired = parents.get(parent)
            if (successRequired) {
              throw new Exception("Parent task " + parent + " failed");
            } // else continue looping over parents
        } else if (status != "succeeded") {
            return false;
        }
    }
    return true;
}


public class TaskStatus {
    public static Map<String, String> taskStatuses = new ConcurrentHashMap<>()

    @NonCPS
    public static void updateStatus(String taskName, String status) {
        if (status != "running" &&
            status != "succeeded" &&
            status != "failed") {
            throw new Exception("Status \"" + status + "\" not legal")
        }
        taskStatuses.put(taskName, status)
    }

    @NonCPS
    public static String getStatus(String taskName) {
        return taskStatuses.get(taskName)
    }
}


def debug(msg) {
    if(DEBUG) {
       println("          [debug]  ${msg}")
    }
}


// for testing only

def _task_statuses_() {
    return TaskStatus.taskStatuses
}


def _set_task_statuses_(statuses) {
    TaskStatus.taskStatuses = new ConcurrentHashMap<>()
    for (entry in statuses.entrySet()) {
        TaskStatus.updateStatus(entry.getKey(), entry.getValue())
    }
}

