// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The pointer argument is relevant although the result is not.

extern void abort(void);
void reach_error(void) { abort(); }

int main(void) {
  int x = 0;
  int *p = &x;
  int old = __atomic_exchange_n(p, 1, 5);
  if (x != 1) {
    reach_error();
  }
  return 0;
}
