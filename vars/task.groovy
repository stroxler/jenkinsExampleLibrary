#!/usr/bin/env groovy 
import com.stroxler.DagRunner


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
    DagRunner dr = new DagRunner()
    return [
        "taskName": taskName,
        "parents": parents,
        "run": { dr.runTask(taskName, parents, block) },
    ]
}


