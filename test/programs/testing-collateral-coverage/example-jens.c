// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_uint();

int main() {
  int y = __VERIFIER_nondet_uint();
  int x = 0;
  int z = 0;

  if (y == 1) {
    x = 1;
  } else {
    z = 1;
  }
    x = 10 / (x - z);
    return x;
}

