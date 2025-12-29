// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2024 The SV-Benchmarks community
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>
#include <stdatomic.h>
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

static RaceMon mon_m = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_n = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_success = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_w = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_x = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_y = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };

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


extern void abort(void);
// void reach_error() { assert(0); }
// Commented out due to possible Syntax Errors
// void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();}
// Commented out due to possible Syntax Errors
}; return; }

// For most memory models (including SC), it is not possible to derive all coherence orders in polynomial time [1].
// In fact, this program (coming from Fig. 6 in [2]) is a counter example to Theorem 2 in [3].

// [1] Florian Furbach, Roland Meyer, Klaus Schneider, Maximilian Senftleben: Memory-Model-Aware Testing: A Unified Complexity Analysis. ACM Trans. Embed. Comput. Syst. 14(4): 63:1-63:25 (2015)
// [2] Liangze Yin, Wei Dong, Wanwei Liu, Ji Wang: On Scheduling Constraint Abstraction for Multi-Threaded Program Verification. IEEE Trans. Software Eng. 46(5): 549-565 (2020)
// [3] Zhihang Sun, Hongyu Fan, Fei He: Consistency-preserving propagation for SMT solving of concurrent program verification. Proc. ACM Program. Lang. 6(OOPSLA2): 929-956 (2022)

// Contributed-by: Hernan Ponce de Leon (Dartagnan Team)

atomic_int m,n,x,y,w;
atomic_int success = 0;

void *p0(void *arg) {
    lock_read(&mon_w);
    int rw = w; // w==2
    unlock_read(&mon_w);
    lock_write(&mon_m);
    m = 1;
    unlock_write(&mon_m);
    lock_read(&mon_x);
    int rx = x; // x==1
    unlock_read(&mon_x);
    if (rw == 2 && rx == 1) {
        lock_write(&mon_success);
        success++;
        unlock_write(&mon_success);
    }
}

void *p1(void *arg) {
    lock_read(&mon_w);
    int rw = w; // w==1
    unlock_read(&mon_w);
    lock_write(&mon_n);
    n = 1;
    unlock_write(&mon_n);
    lock_read(&mon_y);
    int ry = y; // y==2
    unlock_read(&mon_y);
    if (rw == 1 && ry == 2) {
        lock_write(&mon_success);
        success++;
        unlock_write(&mon_success);
    }
}

void *p2(void *arg) {
    lock_write(&mon_x);
    x = 1;
    unlock_write(&mon_x);
    lock_write(&mon_w);
    w = 1;
    unlock_write(&mon_w);
    lock_write(&mon_y);
    y = 1;
    unlock_write(&mon_y);
    lock_read(&mon_n);
    int rn = n; // n == 2
    unlock_read(&mon_n);
    if (rn == 2) {
        lock_write(&mon_success);
        success++;
        unlock_write(&mon_success);
    }
}

void *p3(void *arg) {
    lock_write(&mon_y);
    y = 2;
    unlock_write(&mon_y);
    lock_write(&mon_w);
    w = 2;
    unlock_write(&mon_w);
    lock_write(&mon_x);
    x = 2;
    unlock_write(&mon_x);
    lock_read(&mon_m);
    int rm = m; // m == 2
    unlock_read(&mon_m);
    if (rm == 2) {
        lock_write(&mon_success);
        success++;
        unlock_write(&mon_success);
    }
}

void *p4(void *arg) {
    lock_write(&mon_n);
    n = 2;
    unlock_write(&mon_n);
    lock_read(&mon_w);
    int rw = w; // w==2
    unlock_read(&mon_w);
    if (rw == 2) {
        lock_write(&mon_success);
        success++;
        unlock_write(&mon_success);
    }
}

void *p5(void *arg) {
    lock_write(&mon_m);
    m = 2;
    unlock_write(&mon_m);
    lock_read(&mon_w);
    int rw = w; // w==1
    unlock_read(&mon_w);
    if (rw == 1) {
        lock_write(&mon_success);
        success++;
        unlock_write(&mon_success);
    }
}

int main()
{
    pthread_t t1, t2, t3, t4, t5, t6;

    pthread_create(&t1, NULL, p0, NULL);
    pthread_create(&t2, NULL, p1, NULL);
    pthread_create(&t3, NULL, p2, NULL);
    pthread_create(&t4, NULL, p3, NULL);
    pthread_create(&t5, NULL, p4, NULL);
    pthread_create(&t6, NULL, p5, NULL);

    pthread_join(t1, 0);
    pthread_join(t2, 0);
    pthread_join(t3, 0);
    pthread_join(t4, 0);
    pthread_join(t5, 0);
    pthread_join(t6, 0);

    __VERIFIER_assert(success < 6);

    return 0;
}
