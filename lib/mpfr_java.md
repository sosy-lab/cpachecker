<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# mpfr-java

The mpfr-java project provides Java bindings for MPFR, a C library for multiprecision floating point
operations with correct rounding. Build instructions can be found on the project website
[here](https://github.com/runtimeverification/mpfr-java/). We need to use docker for the build as
the library does not work with more recent version of MPFR.

### Building the package

Download the code:

```
git clone https://github.com/runtimeverification/mpfr-java.git
cd mpfr-java
```

Apply the patch to fix the dependencies and remove the library loader (we can't use the default
system
loader and need to do this ourselves):

```
git apply $CPACHECKER/lib/native/source/mpfr-java.patch
```

Build the package with docker:

```
docker build -t mpfr-bionic .
docker run --rm -it -v `pwd`/output:/output mpfr-bionic $(id -u) $(id -g)
```

### Installation

Simply copy the native library and the jar to the CPAchecker directory:

```
cp output/mpfr_java-1.4.jar $CPACHECKER/lib/mpfr_java.jar
cp output/native-build/.libs/libmpfr_java.so $CPACHECKER/lib/native/x86_64-linux/libmpfr_java.so
```

Then commit the changes and update the repository:

```
git add .
git commit -m "Updated mpfr-java version."
git svn dcommit
```
