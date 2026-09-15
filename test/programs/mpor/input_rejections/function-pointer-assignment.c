// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

void some_function()
{
  const int y = 42;
}
void * start_routine(void *arg)
{
  const int x = 42;
}
int main(void) 
{
  pthread_t id1;
  // function pointer assignment should be rejected on declaration already
  void (*func_ptr)(void) = some_function;
  pthread_create(&id1, NULL, start_routine, NULL);
}

