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


// Expected: true (safe for unreach-label). Machine model: ILP32 (32).
// Each row holds its string with a zero-padded tail; the last row is filled exactly by its
// string, so no terminator is stored beyond the matrix.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char names[3][4] = {"ab", "cde", "fghi"};

  __VERIFIER_assert(sizeof(long) == 4);
  __VERIFIER_assert(sizeof(void *) == 4);
  __VERIFIER_assert(names[0][0] == 'a');
  __VERIFIER_assert(names[0][1] == 'b');
  __VERIFIER_assert(names[0][2] == '\0');
  __VERIFIER_assert(names[0][3] == '\0');
  __VERIFIER_assert(names[1][0] == 'c');
  __VERIFIER_assert(names[1][1] == 'd');
  __VERIFIER_assert(names[1][2] == 'e');
  __VERIFIER_assert(names[1][3] == '\0');
  __VERIFIER_assert(names[2][0] == 'f');
  __VERIFIER_assert(names[2][1] == 'g');
  __VERIFIER_assert(names[2][2] == 'h');
  __VERIFIER_assert(names[2][3] == 'i');

  return 0;
}
