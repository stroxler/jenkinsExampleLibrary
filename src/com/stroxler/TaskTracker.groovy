#!/usr/bin/env groovy
package com.stroxler

import java.util.concurrent.ConcurrentHashMap
import java.util.HashMap
import java.util.Map
import java.util.List


// debugging and printing

@groovy.transform.Field def DEBUG
DEBUG = false
def setDEBUG(debug) {
    this.DEBUG = debug
}


def debug(msg) {
    if(DEBUG) {
       println("          [debug]  ${msg}")
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

def registerSkipped(taskName) {
    TaskStatus.updateStatus(taskName, "skipped")
}


String reportAndSetBuildStatus(currentBuild) {
    List<String> successfulTasks = tasksWithStatus("succeeded")
    List<String> failedTasks = tasksWithStatus("failed")
    List<String> skippedTasks = tasksWithStatus("skipped")

    report = (
        "Task status report\n-----------------\n\n" +
        "Succeeded:\n\t$successfulTasks\n\n" +
        "Failed:\n\t$failedTasks\n\n" +
        "Skipped:\n\t$skippedTasks\n\n"
    )
    if (failedTasks.size > 0) {
        currentBuild.result = 'FAILURE'
    }
    return report
}


List<String> tasksWithStatus(String status) {
    List<String> matches = []
    for (entry in TaskStatus.taskStatuses.entrySet()) {
        String taskName = entry.getKey()
        String taskStatus = entry.getValue()
        if (taskStatus == status) {
            matches.add(taskName)
        }
    }
    return matches
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


boolean parentsReady(String taskName, Map<String,List <String>> parents) {
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


// tools for validating user input to the dag

Map validateAndTransformTaskRuns(List<Map> taskRuns) {
    taskRuns.forEach { tr -> validateTaskRunKeys(tr) }
    Set<String> taskNames = taskRuns.collect({ tr -> tr["taskName"] }).toSet()
    taskRuns.forEach { tr -> validateTaskRunParents(tr, taskNames) }

    Map runMap = taskRuns.collectEntries { taskRun ->
        [taskRun["taskName"], taskRun["run"]]
    }

    return runMap
}


def validateTaskRunParents(Map taskRun, Set<String> taskNames) {
    List<String> parents = collectParents(taskRun["parents"])
    List<String> missing = parents.grep({p -> !taskNames.contains(p)})
    if (missing.size > 0) {
        String name = taskRun["taskName"]
        throw new Error("TaskRun ${name} has nonexistent parents ${missing}")
    }
}


def validateTaskRunKeys(Map taskRun) {
    if (!taskRun.containsKey("taskName")) {
        throw new Error("TaskRun map ${taskRun} is missing taskName, " + 
                        "please use the task() method in your pipeline groovy")
    }
    if (!taskRun.containsKey("parents")) {
        throw new Error("TaskRun map ${taskRun} is missing parents, " + 
                        "please use the task() method in your pipeline groovy")
    }
    if (!taskRun.containsKey("run")) {
        throw new Error("TaskRun map ${taskRun} is missing run, " + 
                        "please use the task() method in your pipeline groovy")
    }
}



List<String> collectParents(List<String> parents) {
    return parents
}


List<String> collectParents(Map<String, List<String>> parents) {
    List<String> out = [];
    for (List<String> parentGroup : parents.values()) {
        out += parentGroup;
    }
    return out;
}


// running tasks

def runTask(taskName, parents, block) {
    try {
        boolean doRun = waitForParents(taskName, parents)
        if (doRun) {
            registerRunning(taskName)
            block()
            registerSucceeded(taskName)
        } else {
            registerSkipped(taskName)
        }
    } catch (Throwable t) {
        printError(t)
        registerFailed(taskName)
    }
}


def printError(Throwable t) {
    StringWriter sw = new StringWriter()
    t.printStackTrace(new PrintWriter(sw))
    println(sw.toString())
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

