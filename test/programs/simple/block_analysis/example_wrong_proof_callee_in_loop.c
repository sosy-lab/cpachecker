// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// Written to reproduce the block structure that makes DSS with
// blockAnalysisType=PATH_BASED report a wrong proof. The same structure occurs in
// coreutils-v9.5-units/seq_cmp_antisymmetry_cover_target, where the shared callee is
// the assume_or_exit() helper that every input generator calls.

extern void abort(void);
extern void exit(int);
extern int __VERIFIER_nondet_int();
void reach_error() { abort(); }

void maybe_exit() {
  if (__VERIFIER_nondet_int()) {
    exit(0);
  }
}

int main() {
  maybe_exit();
  for (int i = 0; i < 5; i++) {
    maybe_exit();
  }
  reach_error();
  return 0;
}
