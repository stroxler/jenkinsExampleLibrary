# Example of a groovy shared library for jenkins

I'm interested in how to write custom jenkins functionality that includes
(isolated) state managment within the execution of a pipeline, with an
eye toward allowing airflow-style DAGs of tasks inside jenkins pipelines.

This plugin is a first step toward exploring this possiblility.

## Development

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

To mostly get around this issue, I wrote a `runtests.py` script that
copies the whole codebase to a temporary directory, stripping out every
instance of `@NonCPS` that I find in `src` files, and then runs
`tests.groovy` with a proper classpath or starts a `groovysh` interactive
shell.

To run tests, execute
```
./runtests.py test
```
To run the interactive `groovysh`, execute
```
./runtests.py groovysh
```

You'll want to `pip install -r dev-requirements.txt`. You may
want to use a virtualenv for this. I apologize for the python; at this point
I am more familiar with python as a bash-like tool, but it would be great
to translate the code to a jvm language.

Note that if you use any jenkins-specific code in the shared library, you
cannot test it this way and you should *not* import groovy classes that use
such code. But if the bulk of your logic is in either non-CPS code that uses
base groovy / java tooling or in CPS code that only uses generic methods
(such as sleep, which jenkins redefines but has similar semantics), you can
test it all using `./runtests.py`.

## Verifying that the shared library works

Here's how I tested it out: I configured my jenkins development instance
to use this plugin, and then made two copies of a job using this groovy
script:
```groovy
@Library("jenkinsExampleLibrary") _

node {
    stage("parallel") {
        parallel([
            "block a": {
                runTask "a-1st", [], {
                    sh "sleep 5"
                    echo "hello from a-1st"
                }
                runTask "a-2nd", [], {
                    sh "sleep 5"
                    echo "hello from a-2nd"
                }
                runTask "a-3rd", [], {
                    sh "sleep 5"
                    echo "hello from a-3rd"
                }
            },
            "block b": {
                runTask "b-1st", ["a-1st"], {
                    echo "hello from b-1st"
                }
                runTask "b-2nd", ["a-3rd"], {
                    echo "hello from b-2nd"
                }
            },
            "block c": {
                runTask "c-1st", [], {
                    echo "hello from c-1st"
                }
                runTask "c-2nd", ["a-2nd"], {
                    echo "hello from c-2nd"
                }
            },
        ])
    }
}
```

Then I ran both pipelines at about the same time.

The test verifies that we are able to run two pipelines at once and
get the desired order of execution in both copies:
  - a1st and c1st start together
  - c1st finishes first
  - a1st finishes, then b1st starts and finishes while a2nd starts
  - a2nd finishes and c2nd starts and finishes while a3rd starts
  - a3rd starts and then b2nd runs

