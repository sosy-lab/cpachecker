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

extern int snprintf(char *str, unsigned long size, const char *format, ...);

// Machine model: LP64. Expected: true for unreach-call and valid-memsafety.
// snprintf writes at most size bytes including a terminator and returns the length of the
// complete output.
int main(void) {
  char complete[6] = {'Z', 'Z', 'Z', 'Z', 'Z', 'Z'};
  char truncated[4] = {'Z', 'Z', 'Z', 'Z'};
  char unchanged[2] = {'Z', 'Z'};

  int completeResult = snprintf(complete, 6UL, "%s", "abc");
  int truncatedResult = snprintf(truncated, 4UL, "%s", "abcdef");
  int unchangedResult = snprintf(unchanged, 0UL, "%s", "ab");

  __VERIFIER_assert(completeResult == 3);
  __VERIFIER_assert(complete[0] == 'a');
  __VERIFIER_assert(complete[1] == 'b');
  __VERIFIER_assert(complete[2] == 'c');
  __VERIFIER_assert(complete[3] == '\0');
  __VERIFIER_assert(complete[4] == 'Z');
  __VERIFIER_assert(complete[5] == 'Z');
  __VERIFIER_assert(truncatedResult == 6);
  __VERIFIER_assert(truncated[0] == 'a');
  __VERIFIER_assert(truncated[1] == 'b');
  __VERIFIER_assert(truncated[2] == 'c');
  __VERIFIER_assert(truncated[3] == '\0');
  __VERIFIER_assert(unchangedResult == 2);
  __VERIFIER_assert(unchanged[0] == 'Z');
  __VERIFIER_assert(unchanged[1] == 'Z');

  return 0;
}
