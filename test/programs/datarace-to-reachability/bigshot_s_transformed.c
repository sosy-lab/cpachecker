// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The CSeq project
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
#include <assert.h>
// void reach_error() { assert(0); }
// Commented out due to possible Syntax Errors

#include <stdlib.h>
#include <pthread.h>
#include <string.h>
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

static RaceMon mon_v = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };

static void lock_read(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  __VERIFIER_atomic_begin();
  __VERIFIER_assert(rm->writers == 0);
  __VERIFIER_atomic_end();
  rm->readers++;
  pthread_mutex_unlock(&rm->m);
}
static void unlock_read(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  rm->readers--;
  pthread_mutex_unlock(&rm->m);
}

static void lock_write(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  __VERIFIER_atomic_begin();
  __VERIFIER_assert(rm->writers == 0 && rm->readers == 0);
  __VERIFIER_atomic_end();
  rm->writers++;
  pthread_mutex_unlock(&rm->m);
}
static void unlock_write(RaceMon* rm) {
  pthread_mutex_lock(&rm->m);
  rm->writers--;
  pthread_mutex_unlock(&rm->m);
}


// void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}}; return; }
// Commented out due to possible Syntax Errors
<<<<<<< HEAD:test/programs/datarace-to-reachability/pthread/bigshot_p_transformed.c

=======
>>>>>>> 66a0585252 (Added new test programs and modified datarace xml):test/programs/datarace-to-reachability/bigshot_s_transformed.c

char *v;

void *thread1(void * arg)
{
  lock_write(&mon_v);
  v = malloc(sizeof(char) * 8);
  unlock_write(&mon_v);
  return 0;
}

void *thread2(void *arg)
{
<<<<<<< HEAD:test/programs/datarace-to-reachability/pthread/bigshot_p_transformed.c
=======
  if (v) {
>>>>>>> 66a0585252 (Added new test programs and modified datarace xml):test/programs/datarace-to-reachability/bigshot_s_transformed.c
  lock_write(&mon_v);
    strcpy(v, "Bigshot");
  unlock_write(&mon_v);
  return 0;
}


int main()
{
  pthread_t t1, t2;

  pthread_create(&t1, 0, thread1, 0);
  pthread_join(t1, 0);

  pthread_create(&t2, 0, thread2, 0);
  pthread_join(t2, 0);

  __VERIFIER_assert(!v || v[0] == 'B');

  return 0;
}
