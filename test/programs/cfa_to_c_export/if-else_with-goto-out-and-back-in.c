// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int __VERIFIER_nondet_int();

int main() {
  int x = __VERIFIER_nondet_int();
  int y;
  if (x >= 0) {
    goto LABEL1;

  LABEL2:;

  } else {
    y = -x;
  }
  int z = y;
  return z;

LABEL1:
  y = x;
  goto LABEL2;
}
