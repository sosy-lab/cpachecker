// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void *malloc (size_t __size) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__malloc__)) ;
extern void abort(void);

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "13-privatized_67-pthread_cond_wait_true.c", 3, __extension__ __PRETTY_FUNCTION__); })); }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: {reach_error();abort();} } }

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
extern void pthread_exit (void *__retval) __attribute__ ((__noreturn__));
extern int pthread_join (pthread_t __th, void **__thread_return);

int* global_ptr;

void *local_dangling_ptr(void *arg) {
  // stack allocated, local_ptr is dangling on return
  int local_var = 0;
  int *local_ptr = &local_var;
  return local_ptr;
}
void *global_dangling_ptr(void *arg) {
  // stack allocated, global_ptr is dangling on return
  int local_var = 0;
  global_ptr = &local_var;
  return global_ptr;
}
void *non_dangling_local_ptr(void *arg) {
  // heap allocated, local_ptr is not dangling on return
  int *local_ptr = (int*) malloc(sizeof(int));
  if (local_ptr == (void*)0) {
    abort(); // safe malloc
  }
  return local_ptr;
}
void *non_dangling_global_ptr(void *arg) {
  // heap allocated, local_ptr is not dangling on return
  global_ptr = (int*) malloc(sizeof(int));
  if (global_ptr == (void*)0) {
    abort(); // safe malloc
  }
  return global_ptr;
}
void *dangling_with_function_call(void* arg) {
  // stack allocated, local_ptr is dangling on return
  int local_var = 0;
  int *local_ptr = &local_var;
  if (local_ptr == (void*)0) {
    abort(); // safe malloc
  }

  // only the dangling pointers of the called function should be reset, so local_ptr remains valid
  called_function();
  if (local_ptr == (void*)0) {
    reach_error();
  }
  return local_ptr;
}
void called_function() {
  // stack allocated, local_ptr is dangling on return
  int local_var = 0;
  int *local_ptr = &local_var;
}

int main() {
  void *local_result = (void*)0;

  pthread_t t1;
  pthread_create(&t1, 0, local_dangling_ptr, 0);
  pthread_join(t1, &local_result);
  if (local_result != (void*)0) {
    reach_error();
  }

  pthread_t t2;
  pthread_create(&t2, 0, global_dangling_ptr, 0);
  pthread_join(t2, &local_result);
  if (local_result != (void*)0) {
    reach_error();
  }

  pthread_t t3;
  pthread_create(&t3, 0, non_dangling_local_ptr, 0);
  pthread_join(t3, &local_result);
  if (local_result == (void*)0) {
    reach_error();
  }

  pthread_t t4;
  pthread_create(&t4, 0, non_dangling_global_ptr, 0);
  pthread_join(t4, &local_result);
  if (local_result == (void*)0) {
    reach_error();
  }

  pthread_t t5;
  pthread_create(&t5, 0, dangling_with_function_call, 0);
  pthread_join(t5, &local_result);
  if (local_result != (void*)0) {
    reach_error();
  }

  return 0;
}
