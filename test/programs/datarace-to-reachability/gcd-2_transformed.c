extern void abort(void);
#include <assert.h>
// void reach_error() { assert(0); }
// Commented out due to possible Syntax or Logic Errors
extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
// Copyright (c) 2015 Michael Tautschnig <michael.tautschnig@qmul.ac.uk>
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/*
VerifyThis ETAPS 2015, Challenge 2

PARALLEL GCD (60 minutes)
=========================

Algorithm description
---------------------

Various parallel GCD algorithms exist. In this challenge, we consider a
simple Euclid-like algorithm with two parallel threads. One thread
subtracts in one direction, the other thread subtracts in the other
direction, and eventually this procedure converges on GCD.


Implementation
--------------

In pseudocode, the algorithm is described as follows:

(
  WHILE a != b DO                                        
      IF a>b THEN a:=a-b ELSE SKIP FI
  OD
||
  WHILE a != b DO                                        
       IF b>a THEN b:=b-a ELSE SKIP FI
  OD
);
OUTPUT a


Verification tasks
------------------

Specify and verify the following behaviour of this parallel GCD algorithm:

Input:  two positive integers a and b
Output: a positive number that is the greatest common divisor of a and b

Feel free to add synchronisation where appropriate, but try to avoid
blocking of the parallel threads.


Sequentialization
-----------------

If your tool does not support reasoning about parallel threads, you may
verify the following pseudocode algorithm:

WHILE a != b DO
    CHOOSE(
         IF a > b THEN a := a - b ELSE SKIP FI,
         IF b > a THEN b := b - a ELSE SKIP FI
    )
OD;
OUTPUT a
*/


extern unsigned int __VERIFIER_nondet_uint();

// void __VERIFIER_assert(int cond) {
//   if (!(cond)) {
//     ERROR: {reach_error();abort();}
//   }
//   return;
// }
// Commented out due to possible Syntax or Logic Errors

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

static RaceMon mon_a = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };
static RaceMon mon_b = { PTHREAD_MUTEX_INITIALIZER, 0, 0 };

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


unsigned a, b;

void __VERIFIER_atomic_dec_a()
{
  if(a>b)
    a=a-b;
}

void __VERIFIER_atomic_dec_b()
{
  if(b>a)
    b=b-a;
}

void* dec_a(void* arg)
{
  (void)arg;

  lock_read(&mon_a);
  lock_read(&mon_b);
  while(a!=b)
  {
    __VERIFIER_atomic_dec_a();
  }
  unlock_read(&mon_b);
  unlock_read(&mon_a);

  return 0;
}

void* dec_b(void* arg)
{
  (void)arg;

  lock_read(&mon_a);
  lock_read(&mon_b);
  while(a!=b)
  {
    __VERIFIER_atomic_dec_b();
  }
  unlock_read(&mon_b);
  unlock_read(&mon_a);

  return 0;
}

unsigned start(unsigned a_in, unsigned b_in)
{
  lock_write(&mon_a);
  a=a_in;
  unlock_write(&mon_a);
  lock_write(&mon_b);
  b=b_in;
  unlock_write(&mon_b);

  pthread_t t1, t2;

  pthread_create(&t1, 0, dec_a, 0);
  pthread_create(&t2, 0, dec_b, 0);

  pthread_join(t1, 0);
  pthread_join(t2, 0);

  lock_read(&mon_a);
  return a;
  unlock_read(&mon_a);
}

void check_gcd(unsigned a_in, unsigned b_in, unsigned gcd)
{
  unsigned guessed_gcd=__VERIFIER_nondet_uint();
  assume_abort_if_not(guessed_gcd>1);
  assume_abort_if_not(a_in%guessed_gcd==0);
  assume_abort_if_not(b_in%guessed_gcd==0);

  __VERIFIER_assert(a_in%gcd==0);
  __VERIFIER_assert(b_in%gcd==0);

  __VERIFIER_assert(gcd>=guessed_gcd);
}

int main()
{
  // for testing with small unwinding bounds
  unsigned a_in=__VERIFIER_nondet_uint(); //=8;
  unsigned b_in=__VERIFIER_nondet_uint(); //=6;

  assume_abort_if_not(a_in>0);
  assume_abort_if_not(b_in>0);
  check_gcd(a_in, b_in, start(a_in, b_in));
  return 0;
}
