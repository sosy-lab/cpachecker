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

struct Point { long coords[2]; char *label; };


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// Each struct element initializes its array member; omitted array elements, members and the
// completely omitted third element are zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct Point points[3] = {{{1, 2}, 0}, {{4}}};

  __VERIFIER_assert(sizeof(long) == 8);
  __VERIFIER_assert(sizeof(void *) == 8);
  __VERIFIER_assert(points[0].coords[0] == 1);
  __VERIFIER_assert(points[0].coords[1] == 2);
  __VERIFIER_assert(points[0].label == 0);
  __VERIFIER_assert(points[1].coords[0] == 4);
  __VERIFIER_assert(points[1].coords[1] == 0);
  __VERIFIER_assert(points[1].label == 0);
  __VERIFIER_assert(points[2].coords[0] == 0);
  __VERIFIER_assert(points[2].coords[1] == 0);
  __VERIFIER_assert(points[2].label == 0);

  return 0;
}
