// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The load-bearing half of loop_handle_safe.c. Same shape — handles created
// with literal indices but joined through t[i] with a loop variable, so the
// join cannot use the fast-path hint and falls back to general candidate-set
// branching — but with a real violation.
//
// This direction is what actually tests that path. The failure modes it guards
// against (a join offered by the reduction but not taken by the transfer
// relation, silently discarding every schedule through that state; or CEGAR
// latching onto a spurious "handle == wrong candidate" fact and refining away a
// feasible schedule) both surface as a wrong TRUE, which a safe program cannot
// detect. Here, x == 1 is reachable iff thread 1 runs before thread 0, so
// losing that interleaving turns the expected FALSE into TRUE.

#include <pthread.h>

extern void reach_error(void);
extern void abort(void);

int x = 0;

void *thread0(void *arg) {
  x = 1;
  return 0;
}

void *thread1(void *arg) {
  x = 2;
  return 0;
}

int main() {
  pthread_t t[2];

  pthread_create(&t[0], 0, thread0, 0);
  pthread_create(&t[1], 0, thread1, 0);

  for (int i = 0; i < 2; i++) {
    pthread_join(t[i], 0);
  }

  if (x == 1) {
    reach_error();
    abort();
  }
  return 0;
}
