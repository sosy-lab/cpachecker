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

struct Text { long before; char text[4]; char after; };


// Expected: true (safe for unreach-label). Machine model: ILP32 (32).
// The string literal exactly fills the member array, so no terminator is stored; the member
// after the array, which is initialized first by its designator, keeps its value.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct Text value = {.after = 'y', .before = -1, .text = "abcd"};

  __VERIFIER_assert(sizeof(long) == 4);
  __VERIFIER_assert(sizeof(void *) == 4);
  __VERIFIER_assert(value.before == -1);
  __VERIFIER_assert(value.text[0] == 'a');
  __VERIFIER_assert(value.text[1] == 'b');
  __VERIFIER_assert(value.text[2] == 'c');
  __VERIFIER_assert(value.text[3] == 'd');
  __VERIFIER_assert(value.after == 'y');

  return 0;
}
