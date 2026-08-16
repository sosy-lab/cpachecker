// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

typedef struct {
  int member;
} Inner;
typedef struct {
  Inner inner;
  int member;
} Outer;
Outer outer;

void * start_routine(void *arg)
{
  return;
}
int main(void) {
  pthread_t id1;
  pthread_create(&id1, NULL, start_routine, NULL);
}

