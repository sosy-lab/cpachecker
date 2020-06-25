// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_int();

int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();

  if ((x < y + 2) && (x > y - 1)) {
    if ((x - 1 < y) == 1) {
      if ((x == y) != 1) {
ERROR:
        return -1;
      }
    }
  }
}
