extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
extern void abort(void);
#include <assert.h>
// void reach_error() { assert(0); }
// Commented out due to possible Syntax or Logic Errors

/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader

   This file is adapted from the example introduced in the paper:
   Thread-Modular Verification for Shared-Memory Programs 
   by Cormac Flanagan, Stephen Freund, Shaz Qadeer.
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

static RaceMon mon_l = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_r = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_w = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
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

// #define assert(e) if (!(e)) ERROR: reach_error()
// Commented out due to possible Syntax or Logic Errors

int w=0, r=0, x, y;

void __VERIFIER_atomic_take_write_lock() {
  assume_abort_if_not(w==0 && r==0);
  w = 1;
} 

void __VERIFIER_atomic_take_read_lock() {
  assume_abort_if_not(w==0);
  r = r+1;
}

void *writer(void *arg) { //writer
  __VERIFIER_atomic_take_write_lock();  
  lock_write(&mon_x);
  x = 3;
  unlock_write(&mon_x);
  lock_write(&mon_w);
  w = 0;
  unlock_write(&mon_w);
  return 0;
}

void *reader(void *arg) { //reader
  int l;
  __VERIFIER_atomic_take_read_lock();
  lock_write(&mon_l);
  lock_read(&mon_x);
  l = x;
  unlock_read(&mon_x);
  unlock_write(&mon_l);
  lock_read(&mon_l);
  lock_write(&mon_y);
  y = l;
  unlock_write(&mon_y);
  unlock_read(&mon_l);
  lock_read(&mon_x);
  lock_read(&mon_y);
  assert(y == x);
  unlock_read(&mon_y);
  unlock_read(&mon_x);
  lock_write(&mon_l);
  lock_read(&mon_r);
  l = r-1;
  unlock_read(&mon_r);
  unlock_write(&mon_l);
  lock_read(&mon_l);
  lock_write(&mon_r);
  r = l;
  unlock_write(&mon_r);
  unlock_read(&mon_l);
  return 0;
}

int main() {
  pthread_t t1, t2, t3, t4;
  pthread_create(&t1, 0, writer, 0);
  pthread_create(&t2, 0, reader, 0);
  pthread_create(&t3, 0, writer, 0);
  pthread_create(&t4, 0, reader, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  pthread_join(t3, 0);
  pthread_join(t4, 0);
  return 0;
}
