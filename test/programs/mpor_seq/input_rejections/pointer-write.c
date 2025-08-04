// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

int * pointer_a;
int x = 42;
int y = -42;
void * start_routine(void * arg)
{
  // pointer aliasing
  pointer_a = &x;
  int * pointer_b;
  pointer_b = pointer_a;
}
int main()
{
  pthread_t t1;
  pthread_create(&t1, 0, start_routine, 0);
  pointer_a = &y;
  return 0;
}
