// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Like struct_assign_datarace_unsafe.c, but the shared struct is _Atomic, so
// the two concurrent whole-struct assignments are atomic operations and do not
// race. The whole-object accesses are still emitted as events, but marked
// atomic and hence excluded from race candidates. Covers "atomic struct
// support": the qualifier sits on the composite type of the whole-object copy.

#include <pthread.h>

_Atomic struct S {
  int a;
  int b;
} s;

_Atomic struct S other = {1, 2};

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
