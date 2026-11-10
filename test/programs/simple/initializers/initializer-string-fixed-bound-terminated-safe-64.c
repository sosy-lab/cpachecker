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
// The explicit fourteen-element bound fits all thirteen characters and their terminator exactly.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  char value[14] = "abcdefghijklm";

  __VERIFIER_assert(sizeof value == 14U);
  __VERIFIER_assert(value[0] == 'a');
  __VERIFIER_assert(value[1] == 'b');
  __VERIFIER_assert(value[2] == 'c');
  __VERIFIER_assert(value[3] == 'd');
  __VERIFIER_assert(value[4] == 'e');
  __VERIFIER_assert(value[5] == 'f');
  __VERIFIER_assert(value[6] == 'g');
  __VERIFIER_assert(value[7] == 'h');
  __VERIFIER_assert(value[8] == 'i');
  __VERIFIER_assert(value[9] == 'j');
  __VERIFIER_assert(value[10] == 'k');
  __VERIFIER_assert(value[11] == 'l');
  __VERIFIER_assert(value[12] == 'm');
  __VERIFIER_assert(value[13] == '\0');

  return 0;
}
