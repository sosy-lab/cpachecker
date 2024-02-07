// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>
#include <alloca.h>

int main() {
  int *ptr = 0;
  {
  ptr = alloca(10*sizeof(int));

  *ptr = 5;
  assert(*ptr == 5);
  }
  free(ptr); // Unsafe
}
