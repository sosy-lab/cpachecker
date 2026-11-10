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

struct numeric_values {
  char c;
  short s;
  int i;
  long l;
  long long ll;
};

struct outer_record {
  int marker;
  struct numeric_values values;
};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The marker is 11; every member of the omitted nested structure is zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct outer_record object = {11};

  __VERIFIER_assert(object.marker == 11);
  __VERIFIER_assert(object.values.c == 0);
  __VERIFIER_assert(object.values.s == 0);
  __VERIFIER_assert(object.values.i == 0);
  __VERIFIER_assert(object.values.l == 0);
  __VERIFIER_assert(object.values.ll == 0);

  return 0;
}
