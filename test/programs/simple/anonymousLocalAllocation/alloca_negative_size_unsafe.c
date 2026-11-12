// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// ILP32 only: unsafe (valid-deref); negative size converts to space for two ints.
int main(void) {
  const long long negative_size = -4294967288LL;
  // Conversion to 32-bit size_t gives 8 bytes.
  int *ptr = __builtin_alloca(negative_size);

  *ptr = 11;
  ptr++;
  *ptr = 22;
  ptr++;
  *ptr = 33; // Invalid write: one past the two-int allocation.
  return 0;
}
