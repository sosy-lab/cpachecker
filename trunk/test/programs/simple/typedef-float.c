// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef float F;

typedef union _U {
   F y;
} U;

void main() {
   U x;
   F a = 0.0f;
   x.y = -a;
ERROR:
   return;
}
