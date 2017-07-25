import com.stroxler.TaskTracker

import runTask

import java.util.Map
import java.util.HashMap
import java.io.PrintWriter
import java.io.StringWriter


tests = [

  "code should compile": { tt ->
  },

  "debug field on TaskTracker should work": { tt ->
    tt.debug("If you see me this is an error")
    tt.setDEBUG(true)
    tt.debug("Hi from debugger")
  },

  "should be able to register task statuses": { tt ->
    ts = tt._task_statuses_()
    // there should be no tasks initially
    assert ts == [:]
    // we should be able to set status to running
    tt.registerRunning("mytask")
    assert ts == ["mytask": "running"]
    // we should be able to set status to succeeded
    tt.registerSucceeded("mytask")
    assert ts == ["mytask": "succeeded"]
    // we should be able to set status to failed
    tt.registerFailed("mytask2")
    assert ts == ["mytask": "succeeded", "mytask2": "failed"]
  },

  "parentsReady should return true if all are ready": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed",
    ])
    boolean actual
    actual = tt.parentsReady("mytask", ["jobA": true])
    assert actual == true
    actual = tt.parentsReady("mytask", ["jobA": true, "jobB": true])
    assert actual == true
    actual = tt.parentsReady("mytask", ["jobA", "jobB"])
    assert actual == true
    // allowing for non-successful parents shouldn't change output
    actual = tt.parentsReady("mytask", ["jobA": false, "jobB": false])
    assert actual == true
  },

  "parentsReady should return false if any not ready": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed",
    ])
    boolean actual
    actual = tt.parentsReady("mytask", ["jobC": true])
    assert actual == false
    actual = tt.parentsReady("mytask", ["jobA": true, "jobC": true])
    assert actual == false
    actual = tt.parentsReady("mytask", ["jobA", "jobC"])
    assert actual == false
    // a job that isn't in the run map at all is not finished
    actual = tt.parentsReady("mytask", ["jobNotMentioned": true])
    assert actual == false
    // allowing for non-successful parents shouldn't change output
    actual = tt.parentsReady("mytask", ["jobA": false, "jobC": false])
    assert actual == false
  },

  "parentsReady should throw an error if any failed": {tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed",
    ])
    assertThrowsError {
        tt.parentsReady("mytask", ["jobD": true])
    }
    assertThrowsError {
        tt.parentsReady("mytask", ["jobA": true, "jobD": true])
    }
    assertThrowsError {
        tt.parentsReady("mytask", ["jobA", "jobD"])
    }
  },

  "parentsReady should return true when successRequired is false": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed",
    ])
    boolean actual
    actual = tt.parentsReady("mytask", ["jobD": false])
    assert actual == true
    actual = tt.parentsReady("mytask", ["jobA": true, "jobD": false])
    assert actual == true
  },

]


def assertThrowsError(f) {
    boolean thrown = false;
    try {
        f()
    } catch (Throwable t) {
        thrown = true;
    }
    assert thrown == true;
}


def runTests() {
    for(String testName: tests.keySet()) {
        runTest(testName)
    }
}


def runTest(String testName, boolean enableDebug = false) {
    dotest = tests[testName]
    println("Running test ${testName} -------------------------------")
    try {
      tt  = new TaskTracker()
      if (enableDebug) {
        tt.setDEBUG(true)
      }
      dotest(tt)
      println("Test ${testName} succeeded ---------------------------")
    } catch (Throwable t) {
      println("Test ${testName} FAILED ------------------------------")
      StringWriter sw = new StringWriter()
      t.printStackTrace(new PrintWriter(sw))
      println(sw.toString())
      println("End of stack trace -----------------------------------")
    }
}


runTests()
