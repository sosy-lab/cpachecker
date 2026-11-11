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

union Overlay { long longs[2]; int ints[4]; };


// Expected: true (safe for unreach-label). Machine model: ILP32 (32).
// The long array of the first member overlays the int array (little endian); which ints hold
// the values depends on the size of long, the remaining bytes of the union are zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  union Overlay value = {{1, 2}};

  __VERIFIER_assert(sizeof(long) == 4);
  __VERIFIER_assert(sizeof(void *) == 4);
  __VERIFIER_assert(value.longs[0] == 1);
  __VERIFIER_assert(value.longs[1] == 2);
  __VERIFIER_assert(value.ints[0] == 1);
  __VERIFIER_assert(value.ints[1] == 2);
  __VERIFIER_assert(value.ints[2] == 0);
  __VERIFIER_assert(value.ints[3] == 0);

  return 0;
}
