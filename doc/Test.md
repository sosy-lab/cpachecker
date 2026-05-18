<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Test Overview
-------------

| What | Where | When |
| ------ | ------ | ------ |
| Standard unit tests for Java code                             | [GitLab CI][] | all pipelines |
| Expensive unit tests for Java code                            | [GitLab CI][] | merge trains + weekly for `main` |
| Configuration checks (smoke test for each config file)        | [GitLab CI][] | all pipelines |
| [Unit tests for JavaScript code](JavascriptTesting.md)        | [GitLab CI][] | all pipelines |
| [Integration tests for JavaScript code](JavascriptTesting.md) | [GitLab CI][] | all pipelines |
| [Integration tests for Python code](PythonStyleGuide.md)      | [GitLab CI][] | all pipelines |
| Large-scale integration tests for many configs (one with witness validation) | [BuildBot][] | on every push/merge to `main` |
| Largest-scale integration tests on whole [SV-Benchmarks][] for few configs (with witness validation) | [BuildBot][] | every few days for `main` |

Tests for other languages are described in the respective linked documentation, other tests are described below.

Integration Tests
-----------------

Integration tests that are executed automatically by the [BuildBot][]
for the main branch are defined by the files `../test/test-sets/integration-*.xml`.
You can also execute these tests directly with [BenchExec],
which is bundled with CPAchecker, e.g.,
`scripts/benchmark.py test/test-sets/integration-simpleTests.xml`.
CPAchecker developers can also request access to the SoSy-Lab cluster for faster execution.

All major projects and configurations within CPAchecker should be part of this test suite.
To add tests for your project or configuration,
please contact the maintainers on the developer mailing list.
Be aware that the integration tests expect that the directory `c`
of the [SV-Benchmarks][] repository
is linked/copied to `../test/programs/benchmarks`.

Smoke tests that automatically run each CPAchecker configuration on a trivial program
are executed with `ant configuration-checks` and in [GitLab CI][].

Unit Tests
----------

Run `ant unit-tests` from the project root directory.
An HTML report with the results will be generated as `JUnit.html`.
Of course the unit tests can also be executed from within your IDE and are executed by [GitLab CI][].

Some particularly expensive tests (which take several minutes)
are disabled by default and can be enabled with
`-DenableExpensiveTests=true` on the command line.

Structure of Tests
------------------

- The directory `test` in the main directory should be used to store (external) regression tests.
  (In old terminology, these would be called integration/system tests.)
  The regression testing script in that directory should execute all system and all unit tests.

- The actual code for the unit tests should go with the code,
  i.e., not in the `test` directory, but besides the code it tests.
  Code for unit tests is like real code.
  If there is a function `X`, then there can be a function `testX`.
  If it becomes too cluttered, do what you normally would do: refactor (have a file `XTest.java` for file `X.java`).
  If the directory becomes too cluttered, start a new directory.
  But I would start having the test code as close as possible to the code it tests.

- To run JUnit test cases automatically, the name of the class containing the test methods has to end with `Test`.
  No other classes should have such a name.

- Utilities specifically for tests should be placed in the package util.test,
  if they are reusable. Feel free to add utilities there.


Hints for Writing Tests
-----------------------

- Several classes have helpful instances for tests, for example:
  - TestLogManager for LogManager (with tests for correct logger usage)
  - TestDataTools.configurationForTest() for Configuration
  - FileLocation.DUMMY
  - CNumericTypes.* for CSimpleType instances
  - For other types, look in the specific class
  
There is no need to mock these types or create your own instances.
For `ShutdownNotifier`, simply use `ShutdownNotifier.createDummy()`.
Other utilities may be found in the package `util.test`.

- Inside tests, you can use the library [Truth](https://google.github.io/truth/) for writing assertions.
  Instead of writing `assertEquals(...)`, you can write `assertThat(...).is...`,
  and `Truth` offers you a wide range of methods for comparing the actual and the expected result
  (depending on the type of the result), which you can easily use with auto completion.
  The major benefit of this way of writing asserts is that you get more helpful failure messages
  that provide more information about the actual result, and that the test code is more readable.
- Examples for tests using `Truth` can be found in the class `AutomatonTest`.

- In particular, try to avoid `assertTrue()`/`assertFalse()` as well as Truths `assertThat(...).isTrue()`/`isFalse()`,
  because you will get unhelpful failure messages such as `expected result was 'true', but is 'false'`
  instead of for example `expected that string '...' contains '...'`.

- For tests that expect an exception to be thrown,
  use JUnit's `org.junit.Assert.assertThrows`
  (statically import this to avoid the Checkstyle warning).
  This method returns the thrown exception,
  so any further checks on the condition can be done with Truth's `assertThat`.

- Sometimes there are tests that make sense to be executed with different values of one or more parameters,
  for example if you have code that depends on the MachineModel and should work with any instance of MachineModel.
  This can be done without duplicating the test code by using JUnits test runner `Parameterized`.
  To use this, add `@RunWith(Parameterized.class)` to your test class,
  provide a public static method annotated with `@Parameters` that returns a `List<Object[]>`
  and add fields that are annotated with `@Parameter`.
  An example for this can be seen in the class `ExpressionValueVisitorTest`.

[BenchExec]: https://github.com/sosy-lab/benchexec
[BuildBot]: https://buildbot.sosy-lab.org/cpachecker/
[GitLab CI]: https://gitlab.com/sosy-lab/software/cpachecker/-/pipelines
[SV-benchmarks]: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
