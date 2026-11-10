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

union c_union {
  char first;
  char second;
};

union s_union {
  short first;
  short second;
};

union i_union {
  int first;
  int second;
};

union l_union {
  long first;
  long second;
};

union ll_union {
  long long first;
  long long second;
};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// GCC diagnoses and discards 22; only the first union member is initialized to 11 and read.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  union c_union c = {11, 22};
  union s_union s = {11, 22};
  union i_union i = {11, 22};
  union l_union l = {11, 22};
  union ll_union ll = {11, 22};

  __VERIFIER_assert(c.first == 11);
  __VERIFIER_assert(s.first == 11);
  __VERIFIER_assert(i.first == 11);
  __VERIFIER_assert(l.first == 11);
  __VERIFIER_assert(ll.first == 11);

  return 0;
}
