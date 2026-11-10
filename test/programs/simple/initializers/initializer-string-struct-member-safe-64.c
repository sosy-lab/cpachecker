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

struct Text { char text[19]; char marker; };


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The text tail and omitted character member are zero-initialized.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct Text value = {"abcdefghijklm"};

  __VERIFIER_assert(value.text[0] == 'a');
  __VERIFIER_assert(value.text[1] == 'b');
  __VERIFIER_assert(value.text[2] == 'c');
  __VERIFIER_assert(value.text[3] == 'd');
  __VERIFIER_assert(value.text[4] == 'e');
  __VERIFIER_assert(value.text[5] == 'f');
  __VERIFIER_assert(value.text[6] == 'g');
  __VERIFIER_assert(value.text[7] == 'h');
  __VERIFIER_assert(value.text[8] == 'i');
  __VERIFIER_assert(value.text[9] == 'j');
  __VERIFIER_assert(value.text[10] == 'k');
  __VERIFIER_assert(value.text[11] == 'l');
  __VERIFIER_assert(value.text[12] == 'm');
  __VERIFIER_assert(value.text[13] == '\0');
  __VERIFIER_assert(value.text[14] == '\0');
  __VERIFIER_assert(value.text[15] == '\0');
  __VERIFIER_assert(value.text[16] == '\0');
  __VERIFIER_assert(value.text[17] == '\0');
  __VERIFIER_assert(value.text[18] == '\0');
  __VERIFIER_assert(value.marker == 0);

  return 0;
}
