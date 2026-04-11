// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>


extern char __VERIFIER_nondet_char(void);


int main() {
  unsigned int uint_from_char = (int) (unsigned int) __VERIFIER_nondet_char(); // Char is -128 to 127 -> after the cast the value range is 0 to 127 and 4294967168 to 4294967295

  // Prevents underflow and restrict range
  if (uint_from_char <= 200) {
    return 0;
  }
  uint_from_char--;

  assert(uint_from_char != 199); // Safe

  assert(uint_from_char != 255); // Safe

  assert(uint_from_char != 4294967295); // Safe

  assert(uint_from_char != 4294967294); // Violated for the char from __VERIFIER_nondet_char() == -1 => after casts, initial uint_from_char == 4294967295

  return 0;
}