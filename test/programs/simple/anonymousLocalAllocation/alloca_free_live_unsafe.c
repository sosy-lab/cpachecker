// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

// ILP32/LP64: unsafe (valid-free); directly freeing alloca storage.
int main(void) {
  int *ptr = __builtin_alloca(sizeof(*ptr));
  free(ptr); // Invalid free: alloca storage.
  return 0;
}
