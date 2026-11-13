// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// ILP32/LP64: unsafe (valid-deref); writing through a pointer that was never initialized.
int main(void) {
  int *pointer;

  *pointer = 5; // Invalid write: the pointer has an indeterminate value.
  return 0;
}
