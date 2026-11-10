// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// GCC diagnoses and discards 33; each unnamed array contains only 11 and 22.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char *c = (char[2]){11, 22, 33};
  short *s = (short[2]){11, 22, 33};
  int *i = (int[2]){11, 22, 33};
  long *l = (long[2]){11, 22, 33};
  long long *ll = (long long[2]){11, 22, 33};

  __VERIFIER_assert(c[0] == 11);
  __VERIFIER_assert(c[1] == 22);
  __VERIFIER_assert(s[0] == 11);
  __VERIFIER_assert(s[1] == 22);
  __VERIFIER_assert(i[0] == 11);
  __VERIFIER_assert(i[1] == 22);
  __VERIFIER_assert(l[0] == 11);
  __VERIFIER_assert(l[1] == 22);
  __VERIFIER_assert(ll[0] == 11);
  __VERIFIER_assert(ll[1] == 22);

  return 0;
}
