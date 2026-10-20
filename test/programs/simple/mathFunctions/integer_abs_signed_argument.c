// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int abs(int);
extern long long llabs(long long);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  int si = -7;
  long long sll = -123456789012345LL;

  __VERIFIER_assert(abs(si) != 7);
  __VERIFIER_assert(llabs(sll) != 123456789012345LL);

  return 0;
}
