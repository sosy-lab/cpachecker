// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2005-2021 University of Tartu & Technische Universität München
//
// SPDX-License-Identifier: MIT

#include <assert.h>
extern void abort(void);
void reach_error() { assert(0); }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }

extern int __VERIFIER_nondet_int();

#include <pthread.h>

void *t_fun(void *arg) {
  int x = __VERIFIER_nondet_int(); // threadenter shouldn't pass value for x here
  __VERIFIER_assert(!(x == 3));
  return NULL;
}

int main(void) {
  int x = 3;

  pthread_t id;
  pthread_create(&id, NULL, t_fun, NULL);

  return 0;
}
