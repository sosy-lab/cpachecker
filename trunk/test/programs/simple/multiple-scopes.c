// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern char __VERIFIER_nondet_char();
extern void __VERIFIER_error();

int main() {
  int b = 0;

  {
    int a = __VERIFIER_nondet_char();

    switch (a) {
    case 1:
      b = 1;
      break;
    case 2:
      return 2;
    default:
      break;
    }
  }
  {
    char a = __VERIFIER_nondet_char();
    switch (a) {
      case 3:
        return 3;
      case 4:
        b++;
        break;
      default:
        break;
    }
  }
  if (b == 2) {
    __VERIFIER_error();
  }
}
