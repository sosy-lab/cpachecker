// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2001-2016, Daniel Kroening, Edmund Clarke
// SPDX-FileCopyrightText: Computer Science Department, Carnegie Mellon University
// SPDX-FileCopyrightText: Computer Science Department, University of Oxford
//
// SPDX-License-Identifier: LicenseRef-BSD-4-Clause-CBMC

extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
extern _Bool __VERIFIER_nondet_bool(void);
extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "mix014_power.oepc_pso.oepc_rmo.oepc.c", 8, __extension__ __PRETTY_FUNCTION__); })); }
void __VERIFIER_assert(int expression) { if (!expression) { ERROR: {reach_error();abort();} }; return; }
extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

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

void * P0(void *arg);
void * P1(void *arg);
void * P2(void *arg);
void * P3(void *arg);
void fence();
void isync();
void lwfence();
int __unbuffered_cnt;
int __unbuffered_cnt = 0;
int __unbuffered_p1_EAX;
int __unbuffered_p1_EAX = 0;
int __unbuffered_p1_EBX;
int __unbuffered_p1_EBX = 0;
int __unbuffered_p2_EAX;
int __unbuffered_p2_EAX = 0;
int __unbuffered_p3_EAX;
int __unbuffered_p3_EAX = 0;
int __unbuffered_p3_EBX;
int __unbuffered_p3_EBX = 0;
int a;
int a = 0;
_Bool a$flush_delayed;
int a$mem_tmp;
_Bool a$r_buff0_thd0;
_Bool a$r_buff0_thd1;
_Bool a$r_buff0_thd2;
_Bool a$r_buff0_thd3;
_Bool a$r_buff0_thd4;
_Bool a$r_buff1_thd0;
_Bool a$r_buff1_thd1;
_Bool a$r_buff1_thd2;
_Bool a$r_buff1_thd3;
_Bool a$r_buff1_thd4;
_Bool a$read_delayed;
int *a$read_delayed_var;
int a$w_buff0;
_Bool a$w_buff0_used;
int a$w_buff1;
_Bool a$w_buff1_used;
_Bool main$tmp_guard0;
_Bool main$tmp_guard1;
int x;
int x = 0;
int y;
int y = 0;
int z;
int z = 0;
_Bool weak$$choice0;
_Bool weak$$choice2;
void * P0(void *arg)
{
  __VERIFIER_atomic_begin();
  a$w_buff1 = a$w_buff0;
  a$w_buff0 = 1;
  a$w_buff1_used = a$w_buff0_used;
  a$w_buff0_used = (_Bool)1;
  __VERIFIER_assert(!(a$w_buff1_used && a$w_buff0_used));
  a$r_buff1_thd0 = a$r_buff0_thd0;
  a$r_buff1_thd1 = a$r_buff0_thd1;
  a$r_buff1_thd2 = a$r_buff0_thd2;
  a$r_buff1_thd3 = a$r_buff0_thd3;
  a$r_buff1_thd4 = a$r_buff0_thd4;
  a$r_buff0_thd1 = (_Bool)1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  x = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  a = a$w_buff0_used && a$r_buff0_thd1 ? a$w_buff0 : (a$w_buff1_used && a$r_buff1_thd1 ? a$w_buff1 : a);
  a$w_buff0_used = a$w_buff0_used && a$r_buff0_thd1 ? (_Bool)0 : a$w_buff0_used;
  a$w_buff1_used = a$w_buff0_used && a$r_buff0_thd1 || a$w_buff1_used && a$r_buff1_thd1 ? (_Bool)0 : a$w_buff1_used;
  a$r_buff0_thd1 = a$w_buff0_used && a$r_buff0_thd1 ? (_Bool)0 : a$r_buff0_thd1;
  a$r_buff1_thd1 = a$w_buff0_used && a$r_buff0_thd1 || a$w_buff1_used && a$r_buff1_thd1 ? (_Bool)0 : a$r_buff1_thd1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}
void * P1(void *arg)
{
  __VERIFIER_atomic_begin();
  x = 2;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_p1_EAX = x;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_p1_EBX = y;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  a = a$w_buff0_used && a$r_buff0_thd2 ? a$w_buff0 : (a$w_buff1_used && a$r_buff1_thd2 ? a$w_buff1 : a);
  a$w_buff0_used = a$w_buff0_used && a$r_buff0_thd2 ? (_Bool)0 : a$w_buff0_used;
  a$w_buff1_used = a$w_buff0_used && a$r_buff0_thd2 || a$w_buff1_used && a$r_buff1_thd2 ? (_Bool)0 : a$w_buff1_used;
  a$r_buff0_thd2 = a$w_buff0_used && a$r_buff0_thd2 ? (_Bool)0 : a$r_buff0_thd2;
  a$r_buff1_thd2 = a$w_buff0_used && a$r_buff0_thd2 || a$w_buff1_used && a$r_buff1_thd2 ? (_Bool)0 : a$r_buff1_thd2;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}
void * P2(void *arg)
{
  __VERIFIER_atomic_begin();
  y = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_p2_EAX = z;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  a = a$w_buff0_used && a$r_buff0_thd3 ? a$w_buff0 : (a$w_buff1_used && a$r_buff1_thd3 ? a$w_buff1 : a);
  a$w_buff0_used = a$w_buff0_used && a$r_buff0_thd3 ? (_Bool)0 : a$w_buff0_used;
  a$w_buff1_used = a$w_buff0_used && a$r_buff0_thd3 || a$w_buff1_used && a$r_buff1_thd3 ? (_Bool)0 : a$w_buff1_used;
  a$r_buff0_thd3 = a$w_buff0_used && a$r_buff0_thd3 ? (_Bool)0 : a$r_buff0_thd3;
  a$r_buff1_thd3 = a$w_buff0_used && a$r_buff0_thd3 || a$w_buff1_used && a$r_buff1_thd3 ? (_Bool)0 : a$r_buff1_thd3;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}
void * P3(void *arg)
{
  __VERIFIER_atomic_begin();
  z = 1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_p3_EAX = z;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  weak$$choice0 = __VERIFIER_nondet_bool();
  weak$$choice2 = __VERIFIER_nondet_bool();
  a$flush_delayed = weak$$choice2;
  a$mem_tmp = a;
  a = !a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a : (a$w_buff0_used && a$r_buff0_thd4 ? a$w_buff0 : a$w_buff1);
  a$w_buff0 = weak$$choice2 ? a$w_buff0 : (!a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a$w_buff0 : (a$w_buff0_used && a$r_buff0_thd4 ? a$w_buff0 : a$w_buff0));
  a$w_buff1 = weak$$choice2 ? a$w_buff1 : (!a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a$w_buff1 : (a$w_buff0_used && a$r_buff0_thd4 ? a$w_buff1 : a$w_buff1));
  a$w_buff0_used = weak$$choice2 ? a$w_buff0_used : (!a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a$w_buff0_used : (a$w_buff0_used && a$r_buff0_thd4 ? (_Bool)0 : a$w_buff0_used));
  a$w_buff1_used = weak$$choice2 ? a$w_buff1_used : (!a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a$w_buff1_used : (a$w_buff0_used && a$r_buff0_thd4 ? (_Bool)0 : (_Bool)0));
  a$r_buff0_thd4 = weak$$choice2 ? a$r_buff0_thd4 : (!a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a$r_buff0_thd4 : (a$w_buff0_used && a$r_buff0_thd4 ? (_Bool)0 : a$r_buff0_thd4));
  a$r_buff1_thd4 = weak$$choice2 ? a$r_buff1_thd4 : (!a$w_buff0_used || !a$r_buff0_thd4 && !a$w_buff1_used || !a$r_buff0_thd4 && !a$r_buff1_thd4 ? a$r_buff1_thd4 : (a$w_buff0_used && a$r_buff0_thd4 ? (_Bool)0 : (_Bool)0));
  __unbuffered_p3_EBX = a;
  a = a$flush_delayed ? a$mem_tmp : a;
  a$flush_delayed = (_Bool)0;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  a = a$w_buff0_used && a$r_buff0_thd4 ? a$w_buff0 : (a$w_buff1_used && a$r_buff1_thd4 ? a$w_buff1 : a);
  a$w_buff0_used = a$w_buff0_used && a$r_buff0_thd4 ? (_Bool)0 : a$w_buff0_used;
  a$w_buff1_used = a$w_buff0_used && a$r_buff0_thd4 || a$w_buff1_used && a$r_buff1_thd4 ? (_Bool)0 : a$w_buff1_used;
  a$r_buff0_thd4 = a$w_buff0_used && a$r_buff0_thd4 ? (_Bool)0 : a$r_buff0_thd4;
  a$r_buff1_thd4 = a$w_buff0_used && a$r_buff0_thd4 || a$w_buff1_used && a$r_buff1_thd4 ? (_Bool)0 : a$r_buff1_thd4;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  __unbuffered_cnt = __unbuffered_cnt + 1;
  __VERIFIER_atomic_end();
  return 0;
}
void fence()
{
}
void isync()
{
}
void lwfence()
{
}
int main()
{
  pthread_t t353;
  pthread_create(&t353, ((void *)0), P0, ((void *)0));
  pthread_t t354;
  pthread_create(&t354, ((void *)0), P1, ((void *)0));
  pthread_t t355;
  pthread_create(&t355, ((void *)0), P2, ((void *)0));
  pthread_t t356;
  pthread_create(&t356, ((void *)0), P3, ((void *)0));
  __VERIFIER_atomic_begin();
  main$tmp_guard0 = __unbuffered_cnt == 4;
  __VERIFIER_atomic_end();
  assume_abort_if_not(main$tmp_guard0);
  __VERIFIER_atomic_begin();
  a = a$w_buff0_used && a$r_buff0_thd0 ? a$w_buff0 : (a$w_buff1_used && a$r_buff1_thd0 ? a$w_buff1 : a);
  a$w_buff0_used = a$w_buff0_used && a$r_buff0_thd0 ? (_Bool)0 : a$w_buff0_used;
  a$w_buff1_used = a$w_buff0_used && a$r_buff0_thd0 || a$w_buff1_used && a$r_buff1_thd0 ? (_Bool)0 : a$w_buff1_used;
  a$r_buff0_thd0 = a$w_buff0_used && a$r_buff0_thd0 ? (_Bool)0 : a$r_buff0_thd0;
  a$r_buff1_thd0 = a$w_buff0_used && a$r_buff0_thd0 || a$w_buff1_used && a$r_buff1_thd0 ? (_Bool)0 : a$r_buff1_thd0;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  main$tmp_guard1 = !(x == 2 && __unbuffered_p1_EAX == 2 && __unbuffered_p1_EBX == 0 && __unbuffered_p2_EAX == 0 && __unbuffered_p3_EAX == 1 && __unbuffered_p3_EBX == 0);
  __VERIFIER_atomic_end();
  __VERIFIER_assert(main$tmp_guard1);
  return 0;
}
