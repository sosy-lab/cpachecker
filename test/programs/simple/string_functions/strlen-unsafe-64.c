// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error(void) __attribute__((__noreturn__));

void __VERIFIER_assert(int condition) {
  if (!condition) {
    __VERIFIER_error();
  }
}

extern unsigned long strlen(const char *s);

// Machine model: LP64. Expected: false for unreach-call, true for valid-memsafety.
// The embedded terminator ends the string after two characters.
int main(void) {
  char embedded[6] = {'a', 'b', '\0', 'd', 'e', '\0'};

  __VERIFIER_assert(strlen("abc") == 3UL);
  // Wrong on purpose: strlen stops at the first terminator.
  __VERIFIER_assert(strlen(embedded) == 5UL);

  return 0;
}
