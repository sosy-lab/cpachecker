// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern __VERIFIER_nondet_int();

int main() {

  signed int a = __VERIFIER_nondet_int();
  signed int b = __VERIFIER_nondet_int();

  if (a > 10) {
    if (b >= 10) {
      a = b + 5;
      b--;

      if (b <= 9 && a <= 30) {

        b++;
        if (b != 10) {
ERROR:
          return -1;
        }
      }
    }
  }
}
