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

### Create the docker image and compile the dependencies

We use docker to compile the dependencies for our build environment:
```
cd $CPACHECKER/lib/native/source/mpfr-java

docker build -t mpfrjava-focal - < ubuntu2004.Dockerfile
docker run -it \
  --mount type=bind,source=$HOME/workspace,target=/workspace \
  --workdir /workspace \
  --user $(id -u):$(id -g) \
  mpfrjava-focal
```

### Copy the MPFR and GMP libraries

The binaries for GMP and MPFR were already compiled by the Docker script. We only need to copy them
over to the CPAchecker folder:
```
cd /dependencies/

cp gmp-6.3.0/.libs/libgmp.so.10.5.0 /workspace/cpachecker/lib/native/x86_64-linux/libgmp.so
cp mpfr-4.2.1/src/.libs/libmpfr.so.6.2.1 /workspace/cpachecker/lib/native/x86_64-linux/libmpfr.so
```

### Compile mpfr-java

We can now compile `mpfr-java` itself. First fetch the source and apply our patch:
```
cd /workspace

git clone https://github.com/runtimeverification/mpfr-java.git
cd mpfr-java
git apply ../cpachecker/lib/native/source/mpfr-java/mpfr-java.patch
```

Then compile and copy the binaries to the CPAchecker directory:
```
mvn install -DskipTests
cp target/native-build/.libs/libmpfr_java-1.4.so /workspace/cpachecker/lib/native/x86_64-linux/libmpfr_java.so
cp target/mpfr_java-1.4.jar /workspace/cpachecker/lib/mpfr_java.jar
```

### Patch the binaries

We still need to patch the binaries. First leave the docker image:

```
exit
```

Then strip the libraries to remove unnecessary debug symbols, and fix the dependencies with
`patchelf`:

```
cd $CPACHECKER/lib/native/x86_64-linux

strip libgmp.so libmpfr.so libmpfr_java.so
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
