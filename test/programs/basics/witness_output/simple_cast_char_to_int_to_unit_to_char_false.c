// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


#include <assert.h>
#include <stdio.h>


extern char __VERIFIER_nondet_char(void);


int main() {
  int int_from_char = (int) (char) __VERIFIER_nondet_char(); // Char is -128 to 127 -> after the cast to int the value range is the same
  int_from_char--;
  int_from_char--;

  unsigned int uint_from_char = int_from_char;

  // Prevents underflow and restrict range
  if (uint_from_char <= 200) {
    return 0;
  }

  if ((char) uint_from_char > 126) {
    return 0;
  }

  if ((char) uint_from_char < 126) {
    return 0;
  }
  uint_from_char--;

  assert(uint_from_char != 199); // Safe

  assert(uint_from_char != 255); // Safe

  assert(uint_from_char != 4294967295); // Safe

  assert(uint_from_char != 4294967294); // Safe

  assert(uint_from_char != 4294967293); // Safe

  assert(uint_from_char != 4294967292); // Safe

  assert((char) uint_from_char != 125); // Violated for the char from __VERIFIER_nondet_char() == -128 => after cast and two decrements uint_from_char == 4294967166 (== 126 as char), then decremented once again

  return 0;
}