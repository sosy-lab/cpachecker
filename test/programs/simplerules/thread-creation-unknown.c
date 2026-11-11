// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The program creates a thread, so no trivial rule can say anything about data races.
extern int pthread_create(unsigned long *, const void *, void *(*)(void *), void *);

int shared = 0;

void *worker(void *arg) {
  shared = 1;
  return arg;
}

int main(void) {
  unsigned long thread;
  pthread_create(&thread, 0, worker, 0);
  shared = 2;
  return shared;
}
