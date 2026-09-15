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

#include <pthread.h>

int g = 0;

pthread_mutex_t A = PTHREAD_MUTEX_INITIALIZER;

int pqueue_init()
{
  g = 0;
  pthread_mutex_init(&A, NULL);
  return (0);
}

int pqueue_put()
{
  pthread_mutex_lock(&A);
  if (g < 1000)
    g++;
  pthread_mutex_unlock(&A);
  return (1);
}

int pqueue_get()
{
  int got = 0;
  pthread_mutex_lock(&A);
  while (g <= 0) {
    __VERIFIER_assert(g == 0);
  }
  __VERIFIER_assert(g != 0);
  if (g > 0) {
    g--;
    got = 1;
    pthread_mutex_unlock(&A);
  } else {
    pthread_mutex_unlock(&A);
  }
  return (got);
}

void *worker(void *arg )
{
  while (1) {
    pqueue_get();
  }
  return NULL;
}

int main(int argc , char **argv )
{
  pthread_t tid;

  pqueue_init();
  pthread_create(& tid, NULL, & worker, NULL);

  for (int i = 1; i < argc; i++) {
    pqueue_put();
  }
  return 0;
}
