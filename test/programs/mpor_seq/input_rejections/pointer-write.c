// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

int * pointer;
int x = 42;
void * start_routine(void * arg)
{
  // pointer aliasing -> not allowed when sequentializing with bit vectors
  pointer = &x;
}
int main()
{
  pthread_t t1;
  pthread_create(&t1, 0, start_routine, 0);
  return 0;
}
