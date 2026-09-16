// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2005-2021 University of Tartu & Technische Universität München
//
// SPDX-License-Identifier: MIT

// Original file:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/goblint-regression/13-privatized_04-priv_multi_true.i?ref_type=tags

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

extern void abort(void);
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "13-privatized_04-priv_multi_true.c", 3, __extension__ __PRETTY_FUNCTION__); })); }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }

typedef struct __pthread_internal_slist
{
  struct __pthread_internal_slist *__next;
} __pthread_slist_t;
struct __pthread_mutex_s
{
  int __lock;
  unsigned int __count;
  int __owner;
  int __kind;
  unsigned int __nusers;
  __extension__ union
  {
    struct
    {
      short __espins;
      short __eelision;
    } __elision_data;
    __pthread_slist_t __list;
  };
};
typedef unsigned long int pthread_t;
typedef union
{
  char __size[4];
  int __align;
} pthread_mutexattr_t;
union pthread_attr_t
{
  char __size[36];
  long int __align;
};
typedef union pthread_attr_t pthread_attr_t;
typedef union
{
  struct __pthread_mutex_s __data;
  char __size[24];
  long int __align;
} pthread_mutex_t;
enum
{
  PTHREAD_MUTEX_TIMED_NP,
  PTHREAD_MUTEX_RECURSIVE_NP,
  PTHREAD_MUTEX_ERRORCHECK_NP,
  PTHREAD_MUTEX_ADAPTIVE_NP
  ,
  PTHREAD_MUTEX_NORMAL = PTHREAD_MUTEX_TIMED_NP,
  PTHREAD_MUTEX_RECURSIVE = PTHREAD_MUTEX_RECURSIVE_NP,
  PTHREAD_MUTEX_ERRORCHECK = PTHREAD_MUTEX_ERRORCHECK_NP,
  PTHREAD_MUTEX_DEFAULT = PTHREAD_MUTEX_NORMAL
};
extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern int pthread_mutex_init (pthread_mutex_t *__mutex,
          const pthread_mutexattr_t *__mutexattr)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_lock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_unlock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern unsigned int sleep (unsigned int __seconds);

int A = 5;
int B = 5;
pthread_mutex_t mutex_A = { { 0, 0, 0, PTHREAD_MUTEX_TIMED_NP, 0, { { 0, 0 } } } };
pthread_mutex_t mutex_B = { { 0, 0, 0, PTHREAD_MUTEX_TIMED_NP, 0, { { 0, 0 } } } };
void *generate(void *arg) {
  int i;
  for (i=1; i<100; i++) {
    pthread_mutex_lock(&mutex_A);
    A = i;
    A = 5;
    pthread_mutex_unlock(&mutex_A);
    sleep(1);
  }
  return ((void *)0);
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
  return ((void *)0);
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
  return ((void *)0);
}
int main () {
  pthread_t t1, t2, t3;
  int i;
  pthread_create(&t1, ((void *)0), generate, ((void *)0));
  pthread_create(&t2, ((void *)0), process, ((void *)0));
  pthread_create(&t3, ((void *)0), dispose, ((void *)0));
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
