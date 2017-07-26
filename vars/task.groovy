#!/usr/bin/env groovy 
import com.stroxler.TaskTracker


def call(map) {
    if (map["taskName"] == null || map["block"] == null) {
        throw new Exception("Cannot create task without taskName and block")
    }
    map["parents"] = map.getOrDefault("parents", [])
    return call(map["taskName"], map["parents"], map["block"])
}

def call(taskName, block) {
    return call(taskName, [:], block)
}


def call(taskName, parents, block) {
    TaskTracker tt = new TaskTracker()
    return [
        "taskName": taskName,
        "parents": parents,
        "run": { tt.runTask(taskName, parents, block) },
    ]
}


