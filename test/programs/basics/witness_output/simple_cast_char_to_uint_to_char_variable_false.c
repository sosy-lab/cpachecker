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
  unsigned int uint_from_char = (unsigned int) __VERIFIER_nondet_char(); // Char is -128 to 127 -> after the cast the value range is 0 to 255

  // Prevents underflow and restrict range
  if (uint_from_char < 255) {
    return 0;
  }
  uint_from_char--; // uint_from_char == 254

  assert(uint_from_char != 199); // Safe

  assert(uint_from_char != 256); // Safe

  assert(uint_from_char != 255); // Safe

  assert(uint_from_char != 127); // Safe

  assert(uint_from_char != -128); // Safe

  assert(uint_from_char != -2); // Safe

  assert(uint_from_char != -1); // Safe

  char back_to_char = (char) uint_from_char; // back_to_char == -2

  assert(back_to_char != 255); // Safe

  assert(back_to_char != 254); // Safe

  assert(back_to_char != 199); // Safe

  assert(back_to_char != 127); // Safe

  assert(back_to_char != 128); // Safe

  assert(back_to_char != -128); // Safe

  assert(back_to_char != -1); // Safe

  assert(back_to_char != -2); // Fails
  
  return 0;
}