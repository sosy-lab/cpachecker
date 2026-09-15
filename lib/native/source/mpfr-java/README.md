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

### Building with Podman

We provide a script to fully automate the build with Podman. In the CPAchecker main directory type:

```
podman run --rm \
    -v $(pwd):/cpachecker:rw \
    -w /cpachecker \
    ubuntu:focal \
    ./lib/native/source/mpfr-java/compile.sh
```

Once the build is complete the needed *.so files will be copied to the cpachecker/lib
folder. From there they can then be pushed to the CPAchecker repository to update the library.

### Updating the Podman image

When a new version of GMP, MPFR or mpfr-java should be used the build script
*/lib/native/source/mpfr/compile.sh* first needs to be updated. Starting in line 14 the necessary
variables can be found:

```
# Versions for GMP, MPFR and mpfr-java that will be used for the build
VERSION_GMP="6.3.0"
VERSION_MPFR="4.2.1"
VERSION_MPFRJAVA="b7f2e4a61f45cab28f5792dc834c5f20802a11cc"
```

Note that the versions of the .so files may also need to updated whenever one of the projects
undergoes a major version change:

```
# Versions for the .so files from GMP, MPFR and mpfr-java
VERSION_LIBGMP="10.5.0"
VERSION_LIBMPFR="6.2.1"
VERSION_LIBMPFRJAVA="1.4"
```