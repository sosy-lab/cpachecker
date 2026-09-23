// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// ILP32/LP64: unsafe (valid-deref); writing to a zero-byte allocation.
int main(void) {
  unsigned char *ptr = __builtin_alloca(0);

  // alloca(0) does not guarantee that it returns 0, just that the memory is invalid
  *ptr = 1; // Invalid write: zero-byte allocation.
  return 0;
}
