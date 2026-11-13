// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
extern void abort(void);
extern int is_small(int);
void reach_error() {}

int main() {
  int x = __VERIFIER_nondet_int();
  if (!(is_small(x) && is_small(x + 1))) abort();
  if (x > 5) {
    reach_error();
  }
  return 0;
}
