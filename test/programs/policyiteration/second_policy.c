// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

extern int __VERIFIER_nondet_int();

int main() {
    char in[11];
    int i = 0;
    int j = 0;
    int c = 0;
    while (__VERIFIER_nondet_int()) {
        j = c;
        i = i * 10U + j;
        c = in[i];
    }
      if (!(i>=0)) {
          goto ERROR;
      }
      return 0;
    ERROR:
      return -1;
}
