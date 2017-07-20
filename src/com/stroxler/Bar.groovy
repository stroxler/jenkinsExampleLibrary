#!/usr/bin/env groovy
package com.stroxler

import java.lang.Thread
import java.util.concurrent.ConcurrentHashMap
import java.util.HashMap
import java.util.Map
import java.util.List


class Globals {
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

    @NonCPS
    public static boolean parentsReady(List<String> parents) {
        for (String parent : parents) {
            String status = Globals.getStatus(parent)
            if (status == "failed") {
                throw new Exception("Parent task " + parent + "failed")
            } else if (status != "succeeded") {
                return false
            }
        }
        return true
    }

    @NonCPS
    public static void registerRunning(String taskName) {
        Globals.updateStatus(taskName, "running")
    }

    @NonCPS
    public static void registerSucceeded(String taskName) {
        Globals.updateStatus(taskName, "succeeded")
    }

    @NonCPS
    public static void registerFailed(String taskName) {
        Globals.updateStatus(taskName, "failed")
    }
}


def debug(msg) {
    /* Uncomment to enable debugging. I should probably find a better
     * way of controlling this (e.g. an env var) */
    //println("          [debug]  ${msg}")
}

def parentsReady(String taskName, List<String> parents) {
    debug("checking parents ${parents} of task ${taskName}, current statuses: ${Globals.taskStatuses}")
    boolean out = Globals.parentsReady(parents)
    debug("are parents ${parents} of task ${taskName} are ready? ${out}")
    return out
}

def waitForParents(String taskName, List<String> parents) {
    while(!parentsReady(taskName, parents)) {
        Thread.sleep(3 * 1000)
    }
}

/*
def waitForParents(taskName, parents) {
    Globals.waitForParents(taskName, parents)
}
*/

def registerRunning(taskName) {
    Globals.updateStatus(taskName, "running")
}

def registerSucceeded(taskName) {
    Globals.updateStatus(taskName, "succeeded")
}

def registerFailed(taskName) {
    Globals.updateStatus(taskName, "failed")
}
