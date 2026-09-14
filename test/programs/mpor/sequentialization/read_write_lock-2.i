// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: The CSeq project
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
extern void abort(void);

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "read_write_lock-2.c", 7, __extension__ __PRETTY_FUNCTION__); })); }
extern void __VERIFIER_atomic_begin(void);
extern void __VERIFIER_atomic_end(void);

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

int w=0, r=0, x, y;
void __VERIFIER_atomic_take_write_lock() {
  assume_abort_if_not(w==0 && r==0);
  w = 1;
}
void __VERIFIER_atomic_take_read_lock() {
  assume_abort_if_not(w==0);
  r = r+1;
}
void *writer(void *arg) {
  __VERIFIER_atomic_take_write_lock();
  __VERIFIER_atomic_begin();
  x = 3;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  w = 0;
  __VERIFIER_atomic_end();
  return 0;
}
void *reader(void *arg) {
  int l;
  __VERIFIER_atomic_take_read_lock();
  __VERIFIER_atomic_begin();
  l = x;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  y = l;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  int ly = y;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  int lx = x;
  __VERIFIER_atomic_end();
  if (!(ly == lx)) ERROR: reach_error();
  __VERIFIER_atomic_begin();
  l = r-1;
  __VERIFIER_atomic_end();
  __VERIFIER_atomic_begin();
  r = l;
  __VERIFIER_atomic_end();
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
