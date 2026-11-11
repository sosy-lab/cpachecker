// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Global variables named like SMT-LIB keywords
extern void abort(void);
extern void reach_error(void);
extern unsigned int __VERIFIER_nondet_uint(void);

int true = 1;
int false = 0;
int select = 2;

int main() {
  unsigned int n = __VERIFIER_nondet_uint();
  int result = true;
  for (unsigned int i = 0; i < n; i++) {
    if (i == select) {
      result = false;
    }
  }
  if (result != true && result != false) {
    reach_error();
    abort();
  }
  return 0;
}
