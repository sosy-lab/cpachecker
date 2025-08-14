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
int z = 0;
void * start_routine(void * arg)
{
  // pointer aliasing
  pointer_a = &x;
  int * pointer_b;
  pointer_b = pointer_a;
  int z = y + *pointer_b;
  *pointer_a = 42;
  *pointer_b = 42 + *pointer_a;
  int * pointer_c;
  pointer_c = pointer_b;
  int * pointer_d;
  pointer_d = &y;
  int xx = pointer_function(pointer_a);
}
int pointer_function(int * param) 
{
  *param = *param + 1;
  int * pointer_e;
  param = pointer_e;
  pointer_e = &z;
  return *param;
}
int main()
{
  pthread_t t1;
  pthread_create(&t1, 0, start_routine, 0);
  pointer_a = &y;
  *pointer_a = 42;
  pointer_a = &x;
  *pointer_a = 42 * 2;
  return 0;
}
