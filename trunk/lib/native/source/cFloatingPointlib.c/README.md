<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

The Java class that `CFloatNativeAPI.h` is generated from
is in the package `org.sosy_lab.cpachecker.util.floatingpoint`.

If you want to change the API remember to apply the changes accordingly in
both files.

If you want introduce major changes to the API, do it in the Java class,
compile it, and generate a new `CFloatNativeAPI.h` via `javah`
from the `.class` file.

Remember to adjust in the `.c` file the java packages for the used classes,
when you decide to move or change the package.

Compile a new shared library object by running `compile.sh` after setting
the `JAVA_HOME` environment variable to your respective JVM, e.g.,
`/usr/lib/jvm/java-8-openjdk-amd64`.
