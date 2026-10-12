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
  char char_var = __VERIFIER_nondet_char(); // 126 and 127 lead to errors
  
  // Restrict range
  if (char_var <= 125) {
    return 0;
  }

  int int_from_char = (signed int) char_var; // Char is -128 to 127, but char_var is 127 or 126

  
  int_from_char = int_from_char + 2; // 128 or 129

  assert(int_from_char != 127); // Safe

  assert(int_from_char != 126); // Safe

  assert(int_from_char != 130); // Safe

  assert(int_from_char != 199); // Safe

  assert(int_from_char != 256); // Safe

  assert(int_from_char != 255); // Safe

  assert(int_from_char != -127); // Safe

  assert(int_from_char != -128); // Safe

  assert(int_from_char != -2); // Safe

  assert(int_from_char != -1); // Safe

  char_var = (char) int_from_char; // char_var -> -128 or -127

  assert(char_var != 126); // Safe

  assert(char_var != 127); // Safe

  assert(char_var != 128); // Safe

  assert(char_var != 129); // Safe

  assert(char_var != 130); // Safe

  assert(char_var != 131); // Safe

  assert(char_var != -1); // Safe

  assert(char_var != -2); // Safe

  assert(char_var != -3); // Safe

  assert(char_var != -124); // Safe

  assert(char_var != -126); // Safe

  assert(char_var != -127); // Violated

  assert(char_var != -128); // Violated, but never reachable for a verifier
  
  return 0;
}