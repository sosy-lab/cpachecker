// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2005-2021 University of Tartu & Technische Universität München
//
// SPDX-License-Identifier: MIT

typedef struct __pthread_internal_slist
{
  struct __pthread_internal_slist *__next;
} __pthread_slist_t;
struct __pthread_mutex_s
{
  int __lock ;
  unsigned int __count;
  int __owner;
  int __kind;

  unsigned int __nusers;
  __extension__ union
  {
    struct { short __espins; short __eelision; } __elision_data;
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
extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern int pthread_join (pthread_t __th, void **__thread_return);
extern int pthread_mutex_lock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int pthread_mutex_unlock (pthread_mutex_t *__mutex)
     __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

extern void abort(void);
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "racemacros.h", 9, __extension__ __PRETTY_FUNCTION__); })); }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }
extern int __VERIFIER_nondet_int();
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
pthread_mutex_t __global_lock = { { 0, 0, 0, 0, 0, { { 0, 0 } } } };
pthread_mutex_t mutex1 = { { 0, 0, 0, 0, 0, { { 0, 0 } } } };
pthread_mutex_t mutex2 = { { 0, 0, 0, 0, 0, { { 0, 0 } } } };
void *t_fun(void *arg) {
  int *p = (int *) arg;
  pthread_mutex_lock(&mutex1);
  do { do { pthread_mutex_lock(&__global_lock); (*p)++; pthread_mutex_unlock(&__global_lock); } while (0); do { pthread_mutex_lock(&__global_lock); (*p)--; pthread_mutex_unlock(&__global_lock); } while (0); } while (0);
  pthread_mutex_unlock(&mutex1);
  return ((void *)0);
}
int main(void) {
  pthread_t id;
  int i = 0;
  pthread_create(&id, ((void *)0), t_fun, (void *) &i);
  pthread_mutex_lock(&mutex2);
  do { pthread_mutex_lock(&__global_lock); __VERIFIER_assert((i) == 0); pthread_mutex_unlock(&__global_lock); } while (0);
  pthread_mutex_unlock(&mutex2);
  pthread_join(id, ((void *)0));
  return 0;
}
