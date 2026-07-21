// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The spawned thread writes the whole struct s (`s = other;`); main joins it
// before reading a field of s. The join orders the write before the read, so
// there is no data race. Ensures the whole-struct write still participates in
// happens-before ordering (it is not spuriously flagged against the joined
// read).

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
  pthread_join(id, 0);
  int x = s.a;
  (void)x;
  return 0;
}
