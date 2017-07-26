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

def registerSkipped(taskName) {
    TaskStatus.updateStatus(taskName, "skipped")
}


def propagateTaskFailures() {
    boolean failed = false
    String failures = ""
    Set<String> acceptableStatuses = ["succeeded", "skipped"].toSet()
    for (entry in TaskStatus.taskStatuses.entrySet()) {
        String taskName = entry.getKey()
        String status = entry.getValue()
        if (!acceptableStatuses.contains(status)) {
           failed = true
           failures += "\nTask ${taskName} had non-successful status ${status}"
        }
    }
    if (failed) {
        throw new Exception("Pipeline had non-successful tasks:" + failures)
    }
}


def waitForParents(taskName, parents) {
    try {
        while(!parentsReady(taskName, parents)) {
            sleep(1)
        }
        return true
    } catch (SkipTask st) {
        debug("Skipping task ${taskName} due to parent statuses}")
        return false
    }
}

boolean parentsReady(String taskName, List<String> parents) {
    Map<String, List<String>> parentMap = ["succeeded": parents]
    parentsReady(taskName, parentMap)
}


boolean parentsReady(String taskName, Map<String,List<String>> parents) {
    debug("checking parents ${parents} of task ${taskName}, " +
          "current statuses: ${TaskStatus.taskStatuses}")
    boolean ready = areParentsReady(parents)
    debug("are parents ${parents} of task ${taskName} are ready? ${ready}")
    return ready
}

// internals, not part of the public api

class SkipTask extends Exception {
    public SkipTask () {}
}


boolean areParentsReady(Map<String, List<String>> parents) {
    return (
        areParentsReady(
            parents.getOrDefault("succeeded", []),
            ["succeeded"].toSet(),
            ["failed", "skipped"].toSet()
        ) &&
        areParentsReady(
            parents.getOrDefault("completed", []),
            ["succeeded", "failed", "skipped"].toSet(),
            [].toSet()
        ) &&
        areParentsReady(
            parents.getOrDefault("failed", []),
            ["failed"].toSet(),
            ["succeeded", "skipped"].toSet()
        )
    )
}


boolean areParentsReady(List<String> parents,
                        Set<String> readyIf,
                        Set<String> skipIf) {
    boolean ready = true
    for (String parent : parents) {
        String status = TaskStatus.getStatus(parent);
        if (!readyIf.contains(status)) {
            ready = false
            if (skipIf.contains(status)) {
                throw new SkipTask()
            }
        }
    }
    return ready
}


public class TaskStatus {
    public static Map<String, String> taskStatuses = new ConcurrentHashMap<>()

    @NonCPS
    public static void updateStatus(String taskName, String status) {
        if (status != "running" &&
            status != "succeeded" &&
            status != "failed" &&
            status != "skipped") {
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

