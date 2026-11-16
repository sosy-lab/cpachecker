// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <pthread.h>
#include <stdatomic.h>

extern void reach_error(void);

int counter = 0;

int main() {
  // The result is assigned to a variable that is never read again. An
  // analysis that drops assignments with an irrelevant left-hand side must
  // still not drop this one, because __atomic_fetch_add also writes to
  // &counter, which the assertion below observes.
  int unused = __atomic_fetch_add(&counter, 1, 5);
  if (__atomic_load_n(&counter, 5) != 1) reach_error();
  return 0;
}
