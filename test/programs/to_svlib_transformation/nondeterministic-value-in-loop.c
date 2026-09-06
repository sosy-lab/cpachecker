// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort();
void reach_error(){}

// The analysis does not know the semantics of __builtin_clz and therefore uses a nondeterministic
// value for the result of every call. Every iteration of the loop takes a new one, so the sum can
// be any value and the error is reachable.
int main() {
  int x = 5;
  int sum = 0;
  for (int i = 0; i < 3; i++) {
    sum += __builtin_clz(x);
  }
  if (sum == 7) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
