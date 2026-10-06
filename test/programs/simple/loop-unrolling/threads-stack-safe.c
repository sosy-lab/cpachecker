// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Two threads that push to and pop from a shared stack in a loop, each guarded by a mutex. The
// branches on the results of push and pop are inside the loops, so they are copied along with
// them. The sequentialization has to keep which branch is taken if the condition holds,
// regardless of the order in which the unrolling copies the edges.
//
// The heuristic counts both loops, so they are unrolled.

#include <pthread.h>

extern void reach_error(void);

pthread_mutex_t m;
int top = 0;

int push(void) {
  if (top == 2) {
    return -1;
  }
  top = top + 1;
  return 0;
}

int pop(void) {
  if (top == 0) {
    return -2;
  }
  top = top - 1;
  return 0;
}

void *producer(void *arg) {
  int i = 0;
  while (i < 2) {
    pthread_mutex_lock(&m);
    if (push() == -1) {
      reach_error();
    }
    pthread_mutex_unlock(&m);
    i = i + 1;
  }
  return 0;
}

void *consumer(void *arg) {
  int i = 0;
  while (i < 2) {
    pthread_mutex_lock(&m);
    if (top > 0) {
      if (pop() == -2) {
        reach_error();
      }
    }
    pthread_mutex_unlock(&m);
    i = i + 1;
  }
  return 0;
}

int main(void) {
  pthread_t id1, id2;
  pthread_mutex_init(&m, 0);
  pthread_create(&id1, 0, producer, 0);
  pthread_create(&id2, 0, consumer, 0);
  pthread_join(id1, 0);
  pthread_join(id2, 0);
  return 0;
}
