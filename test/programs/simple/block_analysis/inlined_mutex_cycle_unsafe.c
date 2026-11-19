// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Cases 0, 1, 2, 3 reach the error: case 2 leaves the mutex locked.
// Premature TRUE reproducer: --dss with distributedSummaries.executorType=SEQUENTIAL,
// distributedSummaries.decomposition.mergeBranchBoundaries=false, and
// distributedSummaries.decomposition.largestHorizontalMerge=1.
extern int __VERIFIER_nondet_int(void);
extern void __VERIFIER_error(void);
int mutex = 1;
void lock(void) {
  if (mutex != 1) __VERIFIER_error();
  mutex = 2;
}
void unlock(void) {
  if (mutex != 2) __VERIFIER_error();
  mutex = 1;
}
int main(void) {
  int state = 0;
  while (__VERIFIER_nondet_int() || state != 0) {
    switch (__VERIFIER_nondet_int()) {
      case 0: if (state == 0) { lock(); unlock(); state++; } break;
      case 1: if (state == 1) { lock(); unlock(); state++; } break;
      case 2: if (state == 2) { lock(); state++; } break;
      case 3: if (state == 3) { lock(); unlock(); state++; } break;
      case 4: if (state == 4) { unlock(); state = 0; } break;
    }
  }
}
