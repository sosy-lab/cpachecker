extern void abort(void);
#include <assert.h>
// void reach_error() { assert(0); }
// Commented out due to possible Syntax or Logic Errors

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader
*/

#include <pthread.h>
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

static RaceMon mon_X = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_b1 = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_b2 = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_x = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_y = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };

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

// #undef assert
// Commented out due to possible Syntax or Logic Errors
// #define assert(e) if (!(e)) ERROR: reach_error()
// Commented out due to possible Syntax or Logic Errors

int x, y;
int b1, b2; // boolean flags
int X; // boolean variable to test mutual exclusion

void *thr1(void *_) {
  while (1) {
    lock_write(&mon_b1);
    b1 = 1;
    unlock_write(&mon_b1);
    lock_write(&mon_x);
    x = 1;
    unlock_write(&mon_x);
    if (y != 0) {
      lock_write(&mon_b1);
      b1 = 0;
      unlock_write(&mon_b1);
      lock_read(&mon_y);
      while (y != 0) {};
      unlock_read(&mon_y);
      continue;
    }
    lock_write(&mon_y);
    y = 1;
    unlock_write(&mon_y);
    if (x != 1) {
      lock_write(&mon_b1);
      b1 = 0;
      unlock_write(&mon_b1);
      lock_read(&mon_b2);
      while (b2 >= 1) {};
      unlock_read(&mon_b2);
      if (y != 1) {
	while (y != 0) {};
	continue;
      }
    }
    break;
  }
  // begin: critical section
  lock_write(&mon_X);
  X = 0;
  unlock_write(&mon_X);
  assert(X <= 0);
  // end: critical section
  lock_write(&mon_y);
  y = 0;
  unlock_write(&mon_y);
  lock_write(&mon_b1);
  b1 = 0;
  unlock_write(&mon_b1);
  return 0;
}

void *thr2(void *_) {
  while (1) {
    lock_write(&mon_b2);
    b2 = 1;
    unlock_write(&mon_b2);
    lock_write(&mon_x);
    x = 2;
    unlock_write(&mon_x);
    if (y != 0) {
      lock_write(&mon_b2);
      b2 = 0;
      unlock_write(&mon_b2);
      lock_read(&mon_y);
      while (y != 0) {};
      unlock_read(&mon_y);
      continue;
    }
    lock_write(&mon_y);
    y = 2;
    unlock_write(&mon_y);
    if (x != 2) {
      lock_write(&mon_b2);
      b2 = 0;
      unlock_write(&mon_b2);
      while (b1 >= 1) {};
      if (y != 2) {
	while (y != 0) {};
	continue;
      }
    }
    break;
  }
  // begin: critical section
  lock_write(&mon_X);
  X = 1;
  unlock_write(&mon_X);
  assert(X >= 1);
  // end: critical section
  lock_write(&mon_y);
  y = 0;
  unlock_write(&mon_y);
  lock_write(&mon_b2);
  b2 = 0;
  unlock_write(&mon_b2);
  return 0;
}

int main() {
  pthread_t t1, t2;
  pthread_create(&t1, 0, thr1, 0);
  pthread_create(&t2, 0, thr2, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  return 0;
}
