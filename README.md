# Example of a groovy shared library for jenkins

I'm interested in how to write custom jenkins functionality that includes
(isolated) state managment within the execution of a pipeline, with an
eye toward allowing airflow-style DAGs of tasks inside jenkins pipelines.

This plugin is a first step toward exploring this possiblility.

## Conclusions and design notes

I'm very satisfied at this point with the overall functionality, so I'm
wrapping up this demo project. Feel free to use it as an example for developing
nontrivial shared groovy library logic for jenkins pipelines.

I think my mini testing tool (see run.py and test.groovy) are a pretty good
solution to the issues of verifying that groovy code will run in jenkins
and get quick turnaround.

The core Dag management system in `src` is a good example of how to code
up serious functionality in a groovy shared library; it's pretty well-tested,
although the code is a bit messy and probably isn't idiomatic.

Moving forward, I'll be working on a similar system for Stitch Fix internal
usage, and the one major design change I intend to make is to switch from
a dict of lists for `parents` to a list of dicts: each parent ought to be a
small dict with its task id and the ending states for which we should
run (as opposed to skip) this task. We could default the states to be
`["SUCCEEDED"]`. This is a finer-grained model that I think would be easier
for users to get accustomed to, and also better supports certain use cases than
what I have here.

## Development (testing)

### Why set up local tests?

It's not as easy as I might like to develop a shared library for the
jenkins pipeline plugin, because of a few factors:
  - If you try to run a task in groovy with any kind of syntax error,
    you get a pretty useless error message fromt he CPS compiler that
    doesn't tell you what went wrong.
  - To make matters worse, you get similar-looking errors if you make some
    kind of CPS mistake (calling a CPS-transformed method from a
    non-transformed one, or using non-serializable objects in CPS methods),
    so it's hard to tell trivial groovy syntax errors from major design
    issues
  - A lot of the key code I need to write has to be `@NonCPS`-protected
  - Because the `@NonCPS` annotation is only defined in jenkins, you can't
    easily just verify the groovy code locally

### How do I run tests?

To mostly get around this issue, I wrote a `run.py` script that
copies the whole codebase to a temporary directory, stripping out every
instance of `@NonCPS` that I find in `src` files, and then runs
`tests.groovy` with a proper classpath or starts a `groovysh` interactive
shell.

To run tests, execute
```
./run.py test
```
To run the interactive `groovysh`, execute
```
./run.py groovysh
```

### Installing the python dependencies

You'll want to `pip install -r dev-requirements.txt`. You may
want to use a virtualenv for this. I apologize for the python; at this point
I am more familiar with python as process-starting scripting tool, but it would
be great to translate the code to groovy or ammonite.

### How the tests work

The tests are basically a hand-rolled test framework at the moment, because
I tried using groovy's junit support and it felt like without a bunch of
customization that I don't know how to do both the code and the output were
less readable than the current code.

Note that if you use any jenkins-specific code in the shared library, you
cannot test it this way unless you extend the setup to include mocking the
jenkins specific code.

## Verifying that the shared library works (basic)

There are a few example Jenkinsfiles in groovy scripts, and there's an
ammonite scala script to upload them to a local jenkins instance
(assumed to be running on port 49001, with a user "dev" having password
"dev" - you can hack the script to modify it, feel free to make a PR that
moves this into a json file or some such).

To use it, run
```
./uploadTestJob.scala complexExample
./uploadTestJob.scala basicExample
```
and navigate to `http://localhost:49001/job/basicExample/` or
`http://localhost:49001/job/complexExample/`.

The example jobs will state in their descriptions both the expected
status after tasks run and the groovy code they contain. The `basicExample`
should end in success, the `complexExample` should end in failure.
