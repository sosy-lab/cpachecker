// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Mutual recursion: the recursive call of is_even happens via is_odd, so a stack
// frame of is_even has to be restored across the frame of is_odd.
extern void reach_error(void);

int is_odd(int n);

int is_even(int n) {
  int local = n + 100;
  if (n == 0) {
    return 1;
  }
  int result = is_odd(n - 1);
  if (local != n + 100) {
    reach_error();
  }
  return result;
}

int is_odd(int n) {
  if (n == 0) {
    return 0;
  }
  return is_even(n - 1);
}

int main(void) {
  if (is_even(10) != 1 || is_even(7) != 0) {
    reach_error();
  }
  return 0;
}
