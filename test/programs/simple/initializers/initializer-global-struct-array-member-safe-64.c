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

struct Nested { char small; long big; };
struct Global { int scalar; struct Nested nested; short values[4]; char *pointer; };

struct Global global = {1, {2, 3}, {4, 5}};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The global struct initializes its nested struct and its array member; the array tail and the
// omitted pointer member are zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  __VERIFIER_assert(sizeof(long) == 8);
  __VERIFIER_assert(sizeof(void *) == 8);
  __VERIFIER_assert(global.scalar == 1);
  __VERIFIER_assert(global.nested.small == 2);
  __VERIFIER_assert(global.nested.big == 3);
  __VERIFIER_assert(global.values[0] == 4);
  __VERIFIER_assert(global.values[1] == 5);
  __VERIFIER_assert(global.values[2] == 0);
  __VERIFIER_assert(global.values[3] == 0);
  __VERIFIER_assert(global.pointer == 0);

  return 0;
}
