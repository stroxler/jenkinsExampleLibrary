#!/usr/bin/env groovy
package com.stroxler

import java.lang.Thread
import java.util.concurrent.ConcurrentHashMap
import java.util.HashMap
import java.util.Map
import java.util.List


class TaskStatus {
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
    /* Uncomment to enable debugging. I should probably find a better
     * way of controlling this (e.g. an env var) */
    println("          [debug]  ${msg}")
}


boolean areParentsReady(List<String> parents) {
    for (String parent : parents) {
        String status = TaskStatus.getStatus(parent)
        if (status == "failed") {
            throw new Exception("Parent task " + parent + "failed")
        } else if (status != "succeeded") {
            return false
        }
    }
    return true
}

def parentsReady(String taskName, List<String> parents) {
    debug("checking parents ${parents} of task ${taskName}, current statuses: ${TaskStatus.taskStatuses}")
    boolean out = areParentsReady(parents)
    debug("are parents ${parents} of task ${taskName} are ready? ${out}")
    return out
}

def waitForParents(String taskName, List<String> parents) {
    while(!parentsReady(taskName, parents)) {
        Thread.sleep(3 * 1000)
    }
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
