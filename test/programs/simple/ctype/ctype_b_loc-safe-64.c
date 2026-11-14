// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// glibc ABI function used by the <ctype.h> macros, e.g. isspace(c) is
// ((*__ctype_b_loc())[(int) (c)] & (unsigned short int) _ISspace).
extern const unsigned short int **__ctype_b_loc(void);

// Dereferences a null pointer if the condition does not hold.
void check(int condition) {
  if (!condition) {
    int *null = 0;
    *null = 0;
  }
}

// Machine model: LP64 (little endian). Expected: true for valid-memsafety.
// The program runs in the "C" locale, as it does not call setlocale().
// Character class bits of glibc for little endian: _ISdigit = 2048, _ISspace = 8192.
int main(void) {
  // The returned pointer is the same for all calls.
  check(__ctype_b_loc() == __ctype_b_loc());

  // The table may be indexed with all values of signed char, unsigned char and EOF (-1).
  unsigned short int firstEntry = (*__ctype_b_loc())[-128];
  unsigned short int eofEntry = (*__ctype_b_loc())[-1];
  unsigned short int lastEntry = (*__ctype_b_loc())[255];

  // Character classes of the "C" locale.
  check(((*__ctype_b_loc())[(int)' '] & 8192U) != 0U);
  check(((*__ctype_b_loc())[(int)'\n'] & 8192U) != 0U);
  check(((*__ctype_b_loc())[(int)'a'] & 8192U) == 0U);
  check(((*__ctype_b_loc())[(int)'7'] & 2048U) != 0U);
  check(((*__ctype_b_loc())[(int)'x'] & 2048U) == 0U);
  check(firstEntry == 0U);
  check(eofEntry == 0U);
  check(lastEntry == 0U);
  check((*__ctype_b_loc())[200] == 0U);

  return 0;
}
