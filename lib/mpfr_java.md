<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# mpfr-java
The [mpfr-java](https://github.com/runtimeverification/mpfr-java) project provides Java bindings for
MPFR, a C library for multiprecision floating point operations with correct rounding. We will first
build the dependencies [GMP](https://gmplib.org) and [MPFR](https://www.mpfr.org/) and then compile
the mpfr-java library itself.

### Building GMP
Download the latest release of GMP from their website:
```
curl -O https://gmplib.org/download/gmp/gmp-6.3.0.tar.xz
tar xf gmp-6.3.0.tar.xz
cd gmp-6.3.0
```
Then build the library and copy it to the CPAchecker folder:
```
./configure
make
chmod 644 .libs/libgmp.so.10.5.0
cp .libs/libgmp.so.10.5.0 $CPACHECKER/lib/native/x86_64-linux/libgmp.so
```

### Building MPFR
Download the source:
```
curl -O https://www.mpfr.org/mpfr-current/mpfr-4.2.1.tar.bz2
tar xf mpfr-4.2.1.tar.bz2 
cd mpfr-4.2.1/
```
Then build the library and copy it to the CPAchecker folder:
```
/.configure
make
chmod 644 src/.libs/libmpfr.so.6.2.1 
cp src/.libs/libmpfr.so.6.2.1 $CPACHECKER/lib/native/x86_64-linux/libmpfr.so
```

### Building mpfr-java
Download the code
```
git clone https://github.com/runtimeverification/mpfr-java.git
cd mpfr-java
```
Apply the patch to fix library loading when mpfr-java is used in CPAchecker:
```
git apply $CPACHECKER/lib/native/source/mpfr-java.patch
```
Then build the package and copy the files to the CPAchecker folder:
```
mvn install -DskipTests
chmod 644 target/native-build/.libs/libmpfr_java-1.4.so
cp target/native-build/.libs/libmpfr_java-1.4.so $CPACHECKER/lib/native/x86_64-linux/libmpfr_java.so
cp target/mpfr_java-1.4.jar $CPACHECKER/lib/mpfr_java.jar
```

### Fix the dependencies
In the CPAchecker folder use patchelf to fix the dependencies:
```
cd lib/native/x86_64-linux
patchelf --set-rpath '$ORIGIN' --replace-needed libgmp.so.10 libgmp.so libmpfr.so
patchelf --set-rpath '$ORIGIN' --replace-needed libmpfr.so.6 libmpfr.so --replace-needed libgmp.so.10 libgmp.so libmpfr_java.so
```

### Upload the libraries
Commit the changes and update the repository:
```
git add .
git commit -m "Updated mpfr-java binaries. This version is based on GMP 6.3.0 and MPFR 4.2.1"
git svn dcommit
```
