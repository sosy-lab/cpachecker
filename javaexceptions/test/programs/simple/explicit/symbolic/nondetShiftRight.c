// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_char();

int main() {
  unsigned char a = __VERIFIER_nondet_char();
  unsigned char b = __VERIFIER_nondet_char();

  a = a | 1;
  b = b | 1;
  a = a << 7;
  b = b << 7;

  if (a == 0) {
    goto ERROR;
  }

  if (a != b) {
ERROR:
    return -1;
  }
}
