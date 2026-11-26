// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
void reach_error() {}

int main() {
  int i;
  int n = __VERIFIER_nondet_int();
  for (i = 0; i < n; i++) ;
  if (1 && ((n > 0 && i == 2) || 0)) {
    reach_error();
  }
  return 0;
}
