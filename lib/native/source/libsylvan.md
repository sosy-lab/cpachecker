<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Steps for building JSylvan:
---------------------------

```
git clone https://github.com/trolando/jsylvan
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64 (adapt to your system)
export C_INCLUDE_PATH=$JAVA_HOME/include/linux:$JAVA_HOME/include:$C_INCLUDE_PATH
mvn package
strip target/libsylvan.so

cp target/libsylvan.so <CPAchecker>/lib/native/x86_64-linux/
cp target/jsylvan.jar <CPAchecker>/lib/
```
