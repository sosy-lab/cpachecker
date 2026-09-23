// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The CSeq project
//
// SPDX-License-Identifier: Apache-2.0

// Original file:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/singleton_with-uninit-problems-b.i?ref_type=tags

extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "singleton_with-uninit-problems.c", 3, __extension__ __PRETTY_FUNCTION__); })); }
typedef unsigned int size_t;
typedef long int wchar_t;

extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;

typedef unsigned long int pthread_t;
union pthread_attr_t
{
  char __size[36];
  long int __align;
};
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern int pthread_join (pthread_t __th, void **__thread_return);

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
