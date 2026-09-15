// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

void some_function(void)
{
    const int y = 42;
}
void call_function(void (*func)(void))
{
    func();
}
void * start_routine(void *arg)
{
  const int x = 42;
}
int main(void)
{
    call_function(some_function);
    pthread_t id1;
    pthread_create(&id1, NULL, start_routine, NULL);
}
