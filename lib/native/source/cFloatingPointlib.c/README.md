<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# CFloatingPoint

The `CFloatingPoint` library is used to run native floating point operations in C from Java.
`CPAchecker` only uses this library for testing and can be run without it.

### Generating `CFloatNativeAPI.h` from the Java source

The Java class that `CFloatNativeAPI.h` is generated from is in the package
`org.sosy_lab.cpachecker.util.floatingpoint`. If you want to change the API remember to apply the
changes accordingly in
both files.

Whenever `CFloatNativeAPI.java` was changed, the C header file needs to be regenerated. To do this,
first building and packaging cpachecker with `ant jar`, and then use `javac -h` to
recreate the C header:

```
javac \
 -h . \
 -cp cpachecker.jar:lib/java/runtime/common.jar \
 src/org/sosy_lab/cpachecker/util/floatingpoint/CFloatNativeAPI.java
```

The new file `org_sosy_lab_cpachecker_util_floatingpoint_CFloatNativeAPI.h ` can be used to update
`CFloatNativeAPI.h`. You'll have to merge the changes yourself to
make sure that the license header and the `#defines` at the start of `CFloatNativeAPI.h` are not
overwritten.

Remember to adjust in the `.c` file the java packages for the used classes,
when you decide to move or change the package.

### Compiling the binary

Compile a new shared library object by running `compile.sh` after setting
the `JAVA_HOME` environment variable to your respective JVM, e.g.,
`/usr/lib/jvm/java-17-openjdk-amd64`.

Once compiled, the binary needs to be copied its final location:

```
mv libFloatingPoints.so ../../x86_64-linux
```

The new version can then be published by pushing it to the repository with `git`.