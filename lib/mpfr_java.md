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

### Downloading mpfr-java and its dependencies

We assume that there is a workspace called *WORKDIR* and that CPAchecker has already been downloaded
into this directory:

```
cd WORKDIR
```

Now download the latest version of GMP and unpack it into the working directory:

```
curl -O https://gmplib.org/download/gmp/gmp-6.3.0.tar.xz
tar xf gmp-6.3.0.tar.xz
```

Then fetch MPFR and unpack it:
```
curl -O https://www.mpfr.org/mpfr-current/mpfr-4.2.1.tar.bz2
tar xf mpfr-4.2.1.tar.bz2
```

Now checkout mpfr-java from github:

```
git clone https://github.com/runtimeverification/mpfr-java.git mpfrjava-1.4
cd mpfrjava-1.4
git checkout b7f2e4a61f45cab28f5792dc834c5f20802a11cc
```

### Compiling mpfr-java

We provide a script to build the mpfr binaries with podman:
```
cd WORKDIR/cpachecker/lib/native/source/mpfr-java
./buildForUbuntu2004.sh WORKDIR \
6.3.0 \
4.2.1 \
1.4
```

The last three arguments are the version numbers for GMP, MPFR and mpfr-java. The build script will
compile all three projects in the container and then installs the libraries under
cpachecker/lib/native where they can be uploaded.

Alternatively you can run the compilation script directly:
```
cd WORKDIR/cpachecker/lib/native/source/mpfr-java
./compile.sh \
WORKDIR\gmp-6.3.0 \
WORKDIR\mpfr-4.2.1 \
WORKDIR\mpfrjava-1.4
```

The last three arguments are paths to GMP, MPFR and mpfr-java which we downloaded earlier.