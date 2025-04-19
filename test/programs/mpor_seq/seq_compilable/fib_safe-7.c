// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2018 The Nidhugg project
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The ESBMC project
//
// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later

#include <assert.h>
#include <pthread.h>

extern void abort(void);
void reach_error() { assert(0); }
void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}}; return; }

int i, j;

extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

int p, q;
void *t1(void *arg) {
  // int k = 0;

  for (p = 0; p < 7; p++) {
    __VERIFIER_atomic_begin();
    i = i + j;
    __VERIFIER_atomic_end();
  }
  // atomic_store(&i, atomic_load(&i) + atomic_load(&j));

  return NULL;
}

void *t2(void *arg) {
  // int k = 0;

  for (q = 0; q < 7; q++) {
    __VERIFIER_atomic_begin();
    j = j + i;
    __VERIFIER_atomic_end();
  }
  // atomic_store(&j, atomic_load(&j) + atomic_load(&i));

  return NULL;
}

int cur = 1, prev = 0, next = 0;
int x;
int a;
int fib() {
  for (x = 0; x < 16; x++) {
    next = prev + cur;
    prev = cur;
    cur = next;
  }
  return prev;
}

int main(int argc, char **argv) {
  pthread_t id1, id2;

  // atomic_init(&i, 1);
  // atomic_init(&j, 1);
  __VERIFIER_atomic_begin();
  i = 1;
  __VERIFIER_atomic_end();
  a = 42;
  __VERIFIER_atomic_begin();
  j = 1;
  __VERIFIER_atomic_end();

  pthread_create(&id1, NULL, t1, NULL);
  pthread_create(&id2, NULL, t2, NULL);

  int correct = fib();

  __VERIFIER_atomic_begin();
  _Bool assert_cond = i <= correct && j <= correct; 
  __VERIFIER_atomic_end();
  __VERIFIER_assert(assert_cond);

  return 0;
}
