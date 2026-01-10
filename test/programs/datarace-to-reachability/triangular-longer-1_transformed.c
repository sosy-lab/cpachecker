// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: 2018 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>

extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

extern void abort(void);
#include <assert.h>
extern void abort(void);

void reach_error(void) { assert(0); }
void __VERIFIER_assert(int cond) {
  if (!cond) { ERROR: { reach_error(); abort(); } }
}

static pthread_mutex_t __verifier_atomic_m = PTHREAD_MUTEX_INITIALIZER;
void __VERIFIER_atomic_begin(void) { pthread_mutex_lock(&__verifier_atomic_m); }
void __VERIFIER_atomic_end(void)   { pthread_mutex_unlock(&__verifier_atomic_m); }

typedef struct {
  pthread_mutex_t m;
  int readers;
  int writers;
} RaceMon;

static RaceMon mon_condI = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_condJ = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_i = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_j = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_k = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_k_1 = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };

static void lock_read(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  __VERIFIER_atomic_begin();
  __VERIFIER_assert(rm->writers == 0);
  rm->readers++;
  __VERIFIER_atomic_end();
  pthread_mutex_unlock(&rm->m);
}
static void unlock_read(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  __VERIFIER_atomic_begin();
  rm->readers--;
  __VERIFIER_atomic_end();
  pthread_mutex_unlock(&rm->m);
}

static void lock_write(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  __VERIFIER_atomic_begin();
  __VERIFIER_assert(rm->writers == 0 && rm->readers == 0);
  rm->writers++;
  __VERIFIER_atomic_end();
  pthread_mutex_unlock(&rm->m);
}
static void unlock_write(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  __VERIFIER_atomic_begin();
  rm->writers--;
  __VERIFIER_atomic_end();
  pthread_mutex_unlock(&rm->m);
}

// void reach_error() { assert(0); }
// Commented out due to possible Syntax or Logic Errors

int i = 3, j = 6;

#define NUM 10
#define LIMIT (2*NUM+6)

void *t1(void *arg) {
  for (int k = 0; k < NUM; k++) {
    __VERIFIER_atomic_begin();
    lock_write(&mon_i);
    lock_read(&mon_j);
    i = j + 1;
    unlock_read(&mon_j);
    unlock_write(&mon_i);
    __VERIFIER_atomic_end();
  }
  pthread_exit(NULL);
}

void *t2(void *arg) {
  for (int k = 0; k < NUM; k++) {
    __VERIFIER_atomic_begin();
    lock_read(&mon_i);
    lock_write(&mon_j);
    j = i + 1;
    unlock_write(&mon_j);
    unlock_read(&mon_i);
    __VERIFIER_atomic_end();
  }
  pthread_exit(NULL);
}

int main(int argc, char **argv) {
  pthread_t id1, id2;

  pthread_create(&id1, NULL, t1, NULL);
  pthread_create(&id2, NULL, t2, NULL);

  __VERIFIER_atomic_begin();
  int condI = i > LIMIT;
  __VERIFIER_atomic_end();

  __VERIFIER_atomic_begin();
  int condJ = j > LIMIT;
  __VERIFIER_atomic_end();

  if (condI || condJ) {
//     ERROR: {reach_error();abort();}
// Commented out due to possible Syntax or Logic Errors
  }

  return 0;
}
