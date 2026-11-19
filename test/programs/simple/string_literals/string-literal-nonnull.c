// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The address of a string literal is non-null on every use, not only on the first one
// (failed with --predicateAnalysis --option cpa.predicate.blk.threshold=1).

extern void abort(void);
void reach_error(void) { abort(); }
extern int __VERIFIER_nondet_int(void);

void check(int c) {
  if (!(c && "message")) {
    reach_error();
  }
}

int main(void) {
  int i = 0;
  int n = __VERIFIER_nondet_int();
  while (i < n) {
    i++;
  }
  check(1);
  if (n > 0 && i < 0) {
    reach_error();
  }
  check(1);
  return 0;
}
