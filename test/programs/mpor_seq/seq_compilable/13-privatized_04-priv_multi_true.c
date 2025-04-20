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

#include<pthread.h>
#include <unistd.h> // sleep


int A = 5;
int B = 5;

pthread_mutex_t mutex_A = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t mutex_B = PTHREAD_MUTEX_INITIALIZER;

void *generate(void *arg) {
  int i;
  for (i=1; i<100; i++) {
    pthread_mutex_lock(&mutex_A);
    A = i;
    A = 5;
    pthread_mutex_unlock(&mutex_A);
    sleep(1);
  }
  return NULL;
}

void *process(void *arg) {
  while (1) {
    pthread_mutex_lock(&mutex_A);
    if (A > 0) {
      A++;
      pthread_mutex_lock(&mutex_B);
      B = A;
      B--;
      pthread_mutex_unlock(&mutex_B);
      A--;
      pthread_mutex_unlock(&mutex_A);
    }
    else
      pthread_mutex_unlock(&mutex_A);
    sleep(2);
  }
  return NULL;
}

void *dispose(void *arg) {
  int p;
  while (1) {
    pthread_mutex_lock(&mutex_B);
    if (B > 0) {
      p = B;
      pthread_mutex_unlock(&mutex_B);
      __VERIFIER_assert(p == 5);
    }
    else
      pthread_mutex_unlock(&mutex_B);
    sleep(5);
  }
  return NULL;
}

int main () {
  pthread_t t1, t2, t3;
  int i;

  pthread_create(&t1, NULL, generate, NULL);
  pthread_create(&t2, NULL, process, NULL);
  pthread_create(&t3, NULL, dispose, NULL);

  for (i=0; i<10; i++) {
    pthread_mutex_lock(&mutex_A);
    pthread_mutex_lock(&mutex_B);

    __VERIFIER_assert(A == B);

    pthread_mutex_unlock(&mutex_B);
    pthread_mutex_unlock(&mutex_A);
    sleep(3);
  }
  return 0;
}
