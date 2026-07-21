// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A whole-struct assignment `s = other;` writes every cell of the shared
// struct s. Two threads run it concurrently, so it is a write/write data race.
// Regression for the bug where whole-struct assignments produced no memory
// event at all and the race was silently missed (reported TRUE).

#include <pthread.h>

struct S {
  int a;
  int b;
};

struct S s;
struct S other = {1, 2};

void *thr(void *arg) {
  (void)arg;
  s = other;
  return 0;
}

int main(void) {
  pthread_t id;
  pthread_create(&id, 0, thr, 0);
  s = other;
  pthread_join(id, 0);
  return 0;
}
