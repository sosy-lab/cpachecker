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
  char values[3];
  char alternative;
};

union s_union {
  short values[3];
  short alternative;
};

union i_union {
  int values[3];
  int alternative;
};

union l_union {
  long values[3];
  long alternative;
};

union ll_union {
  long long values[3];
  long long alternative;
};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The selected array member contains 11 followed by two implicit zeros.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  union c_union c = {.values = {11}};
  union s_union s = {.values = {11}};
  union i_union i = {.values = {11}};
  union l_union l = {.values = {11}};
  union ll_union ll = {.values = {11}};

  __VERIFIER_assert(c.values[0] == 11);
  __VERIFIER_assert(c.values[1] == 0);
  __VERIFIER_assert(c.values[2] == 0);
  __VERIFIER_assert(s.values[0] == 11);
  __VERIFIER_assert(s.values[1] == 0);
  __VERIFIER_assert(s.values[2] == 0);
  __VERIFIER_assert(i.values[0] == 11);
  __VERIFIER_assert(i.values[1] == 0);
  __VERIFIER_assert(i.values[2] == 0);
  __VERIFIER_assert(l.values[0] == 11);
  __VERIFIER_assert(l.values[1] == 0);
  __VERIFIER_assert(l.values[2] == 0);
  __VERIFIER_assert(ll.values[0] == 11);
  __VERIFIER_assert(ll.values[1] == 0);
  __VERIFIER_assert(ll.values[2] == 0);

  return 0;
}
