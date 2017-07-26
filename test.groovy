import com.stroxler.TaskTracker

// we can't actually run these methods without mocking pipeline plugin
// internals, but we can check that they compile
import runTask
import checkPipelineStatus

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

  ("parentsReady should return false if any not ready " +
   "(as long as no skip)"): { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed", "jobE": "skipped",
    ])
    boolean actual
    actual = tt.parentsReady("mytask", ["jobC"])
    assert actual == false
    actual = tt.parentsReady("mytask", ["jobA", "jobC"])
    assert actual == false
    actual = tt.parentsReady("mytask", ["succeeded": ["jobA", "jobC"]])
    assert actual == false
    // a job that isn't in the run map at all is not finished
    actual = tt.parentsReady("mytask", ["jobNotMentioned"])
    assert actual == false
    // allowing for non-successful parents shouldn't change output
    actual = tt.parentsReady("mytask", ["completed": ["jobA", "jobC"]])
    assert actual == false
    // requiring failure shoudn't matter for job C or for an unmentioned job
    actual = tt.parentsReady("mytask", ["failed": ["jobNotMentioned", "jobC"]])
    assert actual == false
  },

  "parentsReady should return true if all succeeded": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed", "jobE": "skipped",
    ])
    boolean actual
    actual = tt.parentsReady("mytask", ["jobA"])
    assert actual == true
    actual = tt.parentsReady("mytask", ["jobA", "jobB"])
    assert actual == true
    actual = tt.parentsReady("mytask", ["succeeded": ["jobA", "jobB"]])
    assert actual == true
  },

  "parentsReady should return true if all completed": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed", "jobE": "skipped",
    ])
    actual = tt.parentsReady("mytask", ["completed": ["jobA", "jobE"]])
    assert actual == true
    actual = tt.parentsReady("mytask", ["completed": ["jobA", "jobD"]])
    assert actual == true
  },

  "parentsReady should return true if all failed (when requested)": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed",
    ])
    actual = tt.parentsReady("mytask", ["failed": ["jobD"]])
    assert actual == true
  },

  "parentsReady should return true with mixed requirments": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed", "jobE": "skipped",
    ])
    actual = tt.parentsReady(
        "mytask",
        ["succeeded": ["jobA"],
         "completed": ["jobB", "jobE"],
         "failed": ["jobD"]])
    assert actual == true
  },

  "parentsReady should throw an error when task should be skipped": {tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded",
        "jobC": "running", "jobD": "failed", "jobE": "skipped",
    ])
    assertThrowsError {
        tt.parentsReady("mytask", ["jobA", "jobE"])
    }
    assertThrowsError {
        tt.parentsReady("mytask", ["jobA", "jobD"])
    }
    assertThrowsError {
        tt.parentsReady("mytask", ["failed": "jobA"])
    }
    assertThrowsError {
        tt.parentsReady("mytask", ["failed": "jobE"])
    }
  },

  "propagateTaskFailures should be silent when appropriate": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded", "jobE": "skipped",
    ])
    tt.propagateTaskFailures()
  },

  "propagateTaskFailures should propagate errors properly": { tt ->
    tt._set_task_statuses_([
        "jobA": "succeeded", "jobB": "succeeded", "jobC": "failed",
    ])
    out = assertThrowsError { tt.propagateTaskFailures() }
    assert out.message.contains(
        "Task jobC had non-successful status failed")
  },

]


def assertThrowsError(f) {
    Throwable thrown = null
    try {
        f()
    } catch (Throwable t) {
        thrown = t;
    }
    assert thrown != null;
    return thrown;
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
