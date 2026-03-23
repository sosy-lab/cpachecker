// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {
  ERROR: return;
}

int is_even(int n);
int is_odd(int n);

int is_even(int n) {
  if (n == 0) {
    return 1;
  } else {
    return is_odd(n - 1);
  }
}

int is_odd(int n) {
  if (n == 0) {
    return 0;
  } else {
    return is_even(n - 1);
  }
}

int main() {
  int n = 5;
  if (is_odd(n) == 1) {
    reach_error();
  }
}