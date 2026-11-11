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

struct Inner { long values[3]; };
struct Outer { char first; long values[3]; struct Inner inner; int last; };


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The array member and the array in the nested struct are initialized element-wise, with a
// designator in the nested one; omitted elements are zero and the following member is kept.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct Outer value = {1, {2, 3}, {{4, [2] = 6}}, 7};

  __VERIFIER_assert(sizeof(long) == 8);
  __VERIFIER_assert(sizeof(void *) == 8);
  __VERIFIER_assert(value.first == 1);
  __VERIFIER_assert(value.values[0] == 2);
  __VERIFIER_assert(value.values[1] == 3);
  __VERIFIER_assert(value.values[2] == 0);
  __VERIFIER_assert(value.inner.values[0] == 4);
  __VERIFIER_assert(value.inner.values[1] == 0);
  __VERIFIER_assert(value.inner.values[2] == 6);
  __VERIFIER_assert(value.last == 7);

  return 0;
}
