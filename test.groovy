import com.stroxler.DagRunner

// we can't actually run these methods without mocking pipeline plugin
// internals, but we can check that they compile
import dag
import task

import java.util.Map
import java.util.HashMap
import java.io.PrintWriter
import java.io.StringWriter


tests = [

  "code should compile": { dr ->
  },

  "debug field on DagRunner should work": { dr ->
    dr.debug("If you see me this is an error")
    dr.setDEBUG(true)
    dr.debug("Hi from debugger")
  },

  "should be able to register task statuses": { dr ->
    def ts = dr._task_statuses_()
    // there should be no tasks initially
    assert ts == [:]
    // we should be able to set status to running
    dr.registerRunning("mytask")
    assert ts == ["mytask": "running"]
    // we should be able to set status to succeeded
    dr.registerSucceeded("mytask")
    assert ts == ["mytask": "succeeded"]
    // we should be able to set status to failed
    dr.registerFailed("mytask2")
    assert ts == ["mytask": "succeeded", "mytask2": "failed"]
  },

  ("parentsReady should return false if any not ready " +
   "(as long as no skip)"): { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ])
    boolean actual
    actual = dr.parentsReady("mytask", ["taskC"])
    assert actual == false
    actual = dr.parentsReady("mytask", ["taskA", "taskC"])
    assert actual == false
    actual = dr.parentsReady("mytask", ["succeeded": ["taskA", "taskC"]])
    assert actual == false
    // a task that isn't in the run map at all is not finished
    actual = dr.parentsReady("mytask", ["taskNotMentioned"])
    assert actual == false
    // allowing for non-successful parents shouldn't change output
    actual = dr.parentsReady("mytask", ["completed": ["taskA", "taskC"]])
    assert actual == false
    // requiring failure shoudn't matter for task C or for an unmentioned task
    actual = dr.parentsReady("mytask", ["failed": ["taskNotMentioned", "taskC"]])
    assert actual == false
  },

  "parentsReady should return true if all succeeded": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ])
    boolean actual
    actual = dr.parentsReady("mytask", ["taskA"])
    assert actual == true
    actual = dr.parentsReady("mytask", ["taskA", "taskB"])
    assert actual == true
    actual = dr.parentsReady("mytask", ["succeeded": ["taskA", "taskB"]])
    assert actual == true
  },

  "parentsReady should return true if all completed": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ])
    boolean actual
    actual = dr.parentsReady("mytask", ["completed": ["taskA", "taskE"]])
    assert actual == true
    actual = dr.parentsReady("mytask", ["completed": ["taskA", "taskD"]])
    assert actual == true
  },

  "parentsReady should return true if all failed (when requested)": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed",
    ])
    boolean actual
    actual = dr.parentsReady("mytask", ["failed": ["taskD"]])
    assert actual == true
  },

  "parentsReady should return true with mixed requirments": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ])
    boolean actual
    actual = dr.parentsReady(
        "mytask",
        ["succeeded": ["taskA"],
         "completed": ["taskB", "taskE"],
         "failed": ["taskD"]])
    assert actual == true
  },

  "parentsReady should throw an error when task should be skipped": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ])
    assertThrowsError {
        dr.parentsReady("mytask", ["taskA", "taskE"])
    }
    assertThrowsError {
        dr.parentsReady("mytask", ["taskA", "taskD"])
    }
    assertThrowsError {
        dr.parentsReady("mytask", ["failed": "taskA"])
    }
    assertThrowsError {
        dr.parentsReady("mytask", ["failed": "taskE"])
    }
  },

  "collectParents should run properly": { dr ->
    List<String> actual
    actual = dr.collectParents(["taskA", "taskB", "taskC"])
    assert actual.toSet() == ["taskA", "taskB", "taskC"].toSet()
    actual = dr.collectParents(["succeeded": ["taskA", "taskB", "taskC"]])
    assert actual.toSet() == ["taskA", "taskB", "taskC"].toSet()
    actual = dr.collectParents(["succeeded": ["taskA", "taskB"],
                                "completed": ["taskC"]])
    assert actual.toSet() == ["taskA", "taskB", "taskC"].toSet()
  },

  "validateAndTransformTaskRuns should throw if a task is malformed": { dr ->
    taskRuns = [
      ["taskName": "taskA", "parents": [:], "run": "A"],
      ["taskName": "taskB", "parents": ["taskA"]],  // missing the "run"
    ]
    def out = assertThrowsError { dr.validateAndTransformTaskRuns(taskRuns) }
    assert out.message.contains("taskName:taskB")
    assert out.message.contains("is missing run")
  },

  "validateAndTransformTaskRuns should throw on nonexistent parents": { dr ->
    taskRuns = [
      ["taskName": "taskA", "parents": [:], "run": "A"],
      ["taskName": "taskB", "parents": ["taskC", "taskD",], "run": "B"],
    ]
    def out = assertThrowsError { dr.validateAndTransformTaskRuns(taskRuns) }
    assert out.message.contains("TaskRun taskB has nonexistent parents")
    assert out.message.contains("[taskC, taskD]")
  },

  "validateAndTransformTaskRuns should work as intended": { dr ->
    // note that we use strings instead of closures to bypass the
    // fact that closures are not comparable in groovy
    taskRuns = [
      ["taskName": "taskA", "parents": [:], "run": "A"],
      ["taskName": "taskB", "parents": ["taskA"], "run": "B"],
      ["taskName": "taskC", "parents": ["taskA", "taskB"], "run": "C"],
    ]
    def out = dr.validateAndTransformTaskRuns(taskRuns)
    assert out == ["taskA": "A", "taskB": "B", "taskC": "C", ]
  },

  "runTask should skip task when parents are not ready": { dr ->
    def initial_statuses = [
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ]
    def status = "not run"
    // check with succeeded parents
    dr._set_task_statuses_(initial_statuses)
    dr.runTask("taskF", ["taskA", "taskD"]) { status = "run" }
    assert status == "not run"
    assert dr._task_statuses_()["taskF"] == "skipped"
    // check with failed parents
    dr._set_task_statuses_(initial_statuses)
    dr.runTask("taskF", ["failed": ["taskA", "taskD"]]) { status = "run" }
    assert status == "not run"
    assert dr._task_statuses_()["taskF"] == "skipped"
  },

  "runTask should run when parents are ready": { dr ->
    def initial_statuses = [
        "taskA": "succeeded", "taskB": "succeeded",
        "taskC": "running", "taskD": "failed", "taskE": "skipped",
    ]
    def status = "not run"
    dr._set_task_statuses_(initial_statuses)
    dr.runTask("taskF", ["succeeded": ["taskA"], "failed": ["taskD"]]) {
        status = "run"
    }
    assert status == "run"
    assert dr._task_statuses_()["taskF"] == "succeeded"
  },

  "runTask should handle failures in block properly": { dr ->
    // manual mock, groovy style, see
    // cartesianproduct.wordpress.com/2011/01/30/redirecting-stdout-in-groovy/
    def stdOut = System.out
    def bufStr = new ByteArrayOutputStream()
    def mockStdOut = new PrintStream(bufStr)
    System.out = mockStdOut
    try {
      def initial_statuses = [
          "taskA": "succeeded", "taskB": "succeeded",
          "taskC": "running", "taskD": "failed", "taskE": "skipped",
      ]
      def status = "not run"
      dr._set_task_statuses_(initial_statuses)
      dr.runTask("taskF", ["succeeded": ["taskA"], "failed": ["taskD"]]) {
          status = "run"
          throw new Error("oops")
      }
      assert status == "run"
      assert dr._task_statuses_()["taskF"] == "failed"
      assert bufStr.toString().contains("oops")
    } finally {
      System.out = stdOut
    }
  },

  "reportAndSetuBuildStatus should be silent when appropriate": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded", "taskE": "skipped",
    ])
    def currentBuild = [result: 'SUCCESS']
    out = dr.reportAndSetBuildStatus(currentBuild)
    assert currentBuild.result == 'SUCCESS'
    assert out.contains("Succeeded:\n\t[taskA, taskB]")
    assert out.contains("Skipped:\n\t[taskE]")
  },

  "reportAndSetuBuildStatus should propagate errors properly": { dr ->
    dr._set_task_statuses_([
        "taskA": "succeeded", "taskB": "succeeded", "taskC": "failed",
    ])
    def currentBuild = [result: 'SUCCESS']
    out = dr.reportAndSetBuildStatus(currentBuild)
    assert currentBuild.result == 'FAILURE'
    assert out.contains("Succeeded:\n\t[taskA, taskB]")
    assert out.contains("Failed:\n\t[taskC]")
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
      dr  = new DagRunner()
      if (enableDebug) {
        dr.setDEBUG(true)
      }
      dotest(dr)
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
