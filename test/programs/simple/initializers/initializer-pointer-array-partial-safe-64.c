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
// The first pointer retains its supplied address; omitted pointer elements are null pointers.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char c_value = 11;
  char *c[3] = {&c_value};
  short s_value = 11;
  short *s[3] = {&s_value};
  int i_value = 11;
  int *i[3] = {&i_value};
  long l_value = 11;
  long *l[3] = {&l_value};
  long long ll_value = 11;
  long long *ll[3] = {&ll_value};

  __VERIFIER_assert(c[0] == &c_value);
  __VERIFIER_assert(c[1] == 0);
  __VERIFIER_assert(c[2] == 0);
  __VERIFIER_assert(s[0] == &s_value);
  __VERIFIER_assert(s[1] == 0);
  __VERIFIER_assert(s[2] == 0);
  __VERIFIER_assert(i[0] == &i_value);
  __VERIFIER_assert(i[1] == 0);
  __VERIFIER_assert(i[2] == 0);
  __VERIFIER_assert(l[0] == &l_value);
  __VERIFIER_assert(l[1] == 0);
  __VERIFIER_assert(l[2] == 0);
  __VERIFIER_assert(ll[0] == &ll_value);
  __VERIFIER_assert(ll[1] == 0);
  __VERIFIER_assert(ll[2] == 0);

  return 0;
}
