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
  int x = __VERIFIER_nondet_uint();

  if (y == 1) {
    return -10;
  } else {
    x = x * 2;
  }
  y = y + 1;
  if (x > 0) {
    x = x - 1;
  } else {
    x = x + 1;
  }
  if (y == 2) {
    x =  -10;
  } else {
    x =  10;
  }
  return y;
}

