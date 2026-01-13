// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

int * ptr;
void * start_routine(void * arg)
{
   int local = 42;
   // binary expression assigned to pointer
   ptr = &local + 0;
}
int main()
{
  pthread_t t1;
  pthread_create(&t1, 0, start_routine, NULL);
  return 0;
}
