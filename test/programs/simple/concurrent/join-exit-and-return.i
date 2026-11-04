// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

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

void *thread_case1(void *arg) {
    pthread_exit((void *)100);
    // This code will never be reached because pthread_exit terminates immediately
    return (void *)999;
}
void inner_exit_function(void) {
    pthread_exit((void *)200);
}
void *thread_case2(void *arg) {
    inner_exit_function();
    return (void *)999;
}
int inner_normal_function(int val) {
    return val * 2; // This only returns control back to thread_case3, NOT the thread itself
}
void *thread_case3(void *arg) {
    int inner_result = inner_normal_function(15); // Returns 30
    // Normal return from the start routine acts as an implicit pthread_exit()
    return (void *)inner_result;
}
int main(void) {
    pthread_t t1, t2, t3;
    void *res1 = (void*) 0;
    void *res2 = (void*) 0;
    void *res3 = (void*) 0;

    pthread_create(&t1, (void*) 0, thread_case1, (void*) 0);
    pthread_join(t1, &res1);
    if (res1 != (void *)100) {
        reach_error();
    }

    pthread_create(&t2, (void*) 0, thread_case2, (void*) 0);
    pthread_join(t2, &res2);
    if (res2 != (void *)200) {
        reach_error();
    }

    pthread_create(&t3, (void*) 0, thread_case3, (void*) 0);
    pthread_join(t3, &res3);
    if (res3 != (void *)30) {
        reach_error();
    }

    return 0;
}