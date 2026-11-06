// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// INT_MIN / -1 and -INT_MIN are outside the range of int, just like an addition that is too
// large. The value of the global variable is the same in every execution, so the rules know it.
int min = -2147483647 - 1;

int main(void) {
  int x = min / -1;
  return x;
}
