// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The CSeq project
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }

#include <stdlib.h>
#include <pthread.h>
#include <string.h>

void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}}; return; }

char *v;

void *thread1(void * arg)
{
  v = malloc(sizeof(char));
  return 0;
}

void *thread2(void *arg)
{
  v[0] = 'X';
  return 0;
}

void *thread3(void *arg)
{
  v[0] = 'Y';
  return 0;
}

void *thread0(void *arg)
{
  pthread_t t1, t2, t3, t4, t5;

  pthread_create(&t1, 0, thread1, 0);
  pthread_join(t1, 0);
  pthread_create(&t2, 0, thread2, 0);  
  pthread_create(&t3, 0, thread3, 0);
  pthread_create(&t4, 0, thread2, 0);
  pthread_create(&t5, 0, thread2, 0);
  pthread_join(t2, 0);
  pthread_join(t3, 0);
  pthread_join(t4, 0);
  pthread_join(t5, 0);

  return 0;
}

int main(void)
{
  pthread_t t;

  pthread_create(&t, 0, thread0, 0);
  pthread_join(t, 0);

  __VERIFIER_assert(v[0] == 'X' || v[0] == 'Y');

  return 0;
}

