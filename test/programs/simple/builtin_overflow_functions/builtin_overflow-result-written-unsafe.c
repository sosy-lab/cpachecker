// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The result has to be written to the object the third argument points to,
// otherwise the analysis wrongly assumes that the old value is kept.

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

struct pair {
  int first;
  int second;
};

// ILP32 and LP64: unsafe for unreach-label, but safe for valid-memsafety and no-overflow.
int main(void) {
  struct pair p = {0, 0};
  int overflow = __builtin_add_overflow(1, 2, &p.second);
  __VERIFIER_assert(overflow == 0);
  __VERIFIER_assert(p.first == 0);
  // Wrong: p.second is 3.
  __VERIFIER_assert(p.second == 0);
  return 0;
}
