// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// glibc ABI functions used by tolower() and toupper() of <ctype.h>, e.g. tolower(c) is
// (c >= -128 && c < 256 ? (*__ctype_tolower_loc())[c] : c).
extern const int **__ctype_tolower_loc(void);
extern const int **__ctype_toupper_loc(void);

// Dereferences a null pointer if the condition does not hold.
void check(int condition) {
  if (!condition) {
    int *null = 0;
    *null = 0;
  }
}

// Machine model: LP64. Expected: true for valid-memsafety.
// The program runs in the "C" locale, as it does not call setlocale().
int main(void) {
  // The returned pointers are the same for all calls.
  check(__ctype_tolower_loc() == __ctype_tolower_loc());
  check(__ctype_toupper_loc() == __ctype_toupper_loc());

  // Only letters are converted.
  check((*__ctype_tolower_loc())['A'] == 'a');
  check((*__ctype_tolower_loc())['a'] == 'a');
  check((*__ctype_tolower_loc())['1'] == '1');
  check((*__ctype_toupper_loc())['z'] == 'Z');
  check((*__ctype_toupper_loc())['Z'] == 'Z');
  check((*__ctype_toupper_loc())[200] == 200);

  // The tables may be indexed with all values of signed char, unsigned char and EOF (-1).
  // Negative signed char values are mapped to the unsigned char values, EOF to EOF.
  check((*__ctype_tolower_loc())[-128] == 128);
  check((*__ctype_toupper_loc())[-2] == 254);
  check((*__ctype_tolower_loc())[-1] == -1);
  check((*__ctype_toupper_loc())[-1] == -1);
  check((*__ctype_tolower_loc())[255] == 255);

  return 0;
}
