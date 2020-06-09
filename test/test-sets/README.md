<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

This directory contains test and benchmark suites
for various configurations of CPAchecker.
[BenchExec](https://github.com/sosy-lab/benchexec)
can be used to execute them.
We bundle BenchExec and provide it as `scripts/benchmark.py`,
also cf. [doc/Benchmark.md](../../doc/Benchmark.md).

The definition files assume that the directory `c`
of the [SV-Benchmarks repository](https://github.com/sosy-lab/sv-benchmarks/tree/master/c)
is checked out in the directory `../programs/benchmarks`
(i.e., `test/programs/benchmarks` from the project root).

All files starting with `integration-` are used
by our continuous-integration system
[BuildBot](https://buildbot.sosy-lab.org/buildbot/waterfall).
and executed for commits on trunk
(with `integration-nightly-` being executed every few days).
However, note that new files won't get picked up by BuildBot automatically,
please contact the maintainers for new integration tests.
