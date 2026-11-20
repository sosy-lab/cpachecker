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

// Machine model: LP64. Expected: false for valid-memsafety (valid-deref).
// The table only has entries for the indices -128 to 255.
int main(void) {
  const unsigned short int *table = *__ctype_b_loc();

  // Invalid: 256 is beyond the last entry of the table.
  unsigned short int entry = table[256];

  return entry;
}
