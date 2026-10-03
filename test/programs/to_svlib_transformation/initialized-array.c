// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The stores that initialize the large array, and the assignments of constants, are shortened.
int weights_of_the_first_layer[1000] = {1, 2, 3, 4, 5, 6, 7, 8};

int main() {
  int a;
  int b;
  int c;
  a = 1;
  b = 2;
  c = 3;
  if (weights_of_the_first_layer[7] != 8 || weights_of_the_first_layer[999] != 0
      || a + b + c != 6) {
    ERROR: goto ERROR;
  }
  return 0;
}
