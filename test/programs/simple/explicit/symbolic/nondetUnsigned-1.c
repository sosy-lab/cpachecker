// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_char();

int main() {
  signed char b = __VERIFIER_nondet_char();

  if (b < 0) {
    b = -b;
  }

  unsigned char a = b;

  // set msb to 1;
  b = b | 128;
  a = a | 128;

  if (a != b) {
ERROR:
    return -1;

  } else {
    return 0;
  }
}
