// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


// Violation of valid-deref (in ILP32 and LP64) due to dereferencing a literal that has been cast to a pointer. 
// This program is part of a memsafety violation witness v2 test! Don't change it without changing the witness-test as well!
int main() {

  // Add some code before the violation
  int ptr = 0;
  ptr++;

  ptr = ptr * ptr + ptr;

  free(*(int *)ptr); // Violation. But not due to freeing anything, but dereferencing a number cast to a pointer

  return 0;
}
