// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2021 F. Schuessele <schuessf@informatik.uni-freiburg.de>
// SPDX-FileCopyrightText: 2021 D. Klumpp <klumpp@informatik.uni-freiburg.de>
//
// SPDX-License-Identifier: LicenseRef-BSD-3-Clause-Attribution-Vandikas

typedef unsigned long int pthread_t;

union pthread_attr_t
{
  char __size[36];
  long int __align;
};
typedef union pthread_attr_t pthread_attr_t;

extern void __assert_fail(const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "chl-match-symm.wvr.c", 21, __extension__ __PRETTY_FUNCTION__); }
extern int pthread_create (pthread_t *__restrict __newthread,
      const pthread_attr_t *__restrict __attr,
      void *(*__start_routine) (void *),
      void *__restrict __arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3)));
extern int pthread_join (pthread_t __th, void **__thread_return);

extern int  __VERIFIER_nondet_int(void);
extern _Bool __VERIFIER_nondet_bool(void);
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}

int score_0, seq_1_start_1, seq_2_start_2, score_3, seq_1_start_4, seq_2_start_5, result_6, result_7;

int plus(int a, int b);

void* thread1(void* _argptr) {
  int s1 = plus(seq_1_start_1, seq_2_start_2);
  int s2 = plus(seq_1_start_4, seq_2_start_5);
  result_6 = score_0 > score_3 ? -1 : (score_0 < score_3 ? 1 : (s1 < s2 ? -1 : (s1 > s2 ? 1 : 0)));

  return 0;
}

void* thread2(void* _argptr) {
  int s1 = plus(seq_1_start_4, seq_2_start_5);
  int s2 = plus(seq_1_start_1, seq_2_start_2);
  result_7 = score_3 > score_0 ? -1 : (score_3 < score_0 ? 1 : (s1 < s2 ? -1 : (s1 > s2 ? 1 : 0)));

  return 0;
}

int main() {
  pthread_t t1, t2;
  
  score_0 = __VERIFIER_nondet_int();
  seq_1_start_1 = __VERIFIER_nondet_int();
  seq_2_start_2 = __VERIFIER_nondet_int();
  score_3 = __VERIFIER_nondet_int();
  seq_1_start_4 = __VERIFIER_nondet_int();
  seq_2_start_5 = __VERIFIER_nondet_int();
  
  // main method
  pthread_create(&t1, 0, thread1, 0);
  pthread_create(&t2, 0, thread2, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);

  assume_abort_if_not(!(result_6 < 0 && result_7 > 0));
  assume_abort_if_not(!(result_6 > 0 && result_7 < 0));
  assume_abort_if_not(!(result_6 == 0 && result_7 == 0));
  reach_error();

  return 0;
}

int plus(int a, int b) {
  assume_abort_if_not(b >= 0 || a >= -2147483648 - b);
  assume_abort_if_not(b <= 0 || a <= 2147483647 - b);
  return a + b;
}
