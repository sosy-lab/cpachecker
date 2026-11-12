// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A program without overflows that calls a function through a function pointer.
// The variables in scope are unknown at the CFA nodes created for these calls,
// cf. #1762.

extern long long __VERIFIER_nondet_longlong(void);

static void sink(long long data) {
  if (data > 0 && data < 0x7fffffffffffffffLL / 2) {
    long long result = data * 2;
  }
}

int main(void) {
  void (*funcPtr)(long long) = sink;
  long long data = 2;
  funcPtr(data);
  data = __VERIFIER_nondet_longlong();
  funcPtr(data);
  return 0;
}
