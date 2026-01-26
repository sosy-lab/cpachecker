// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>
int x;
int x = 1;
extern void __assert_fail(const char *__assertion, const char *__file, unsigned int __line, const char *__function);
int printk(const char *arg0, ...) {
  return __VERIFIER_nondet_int();
}
void *task1(void *arg) {
    int hello = 42;
    if (hello == 21 + 21) {
        x = 0;
    } else {
        x++;
    }
    x = 0;
    x++;
    printk("hello!");
    printk("hello", "my", "friend", ":)");
}
void *task2(void *arg) {
    x++;
    x++;
    const int y;
    y = 42;
}
const int global_const = 0;
int main() {
    const int local_const = 7;
    if (x != 1) {
      __assert_fail("0", "simple_two.c", 24, __extension__ __PRETTY_FUNCTION__);
    }
    x = 0;
    pthread_t id1, id2;
    pthread_create(&id1, (void *) 0, task1, (void *) 0);
    pthread_create(&id2, (void *) 0, task2, (void *) 0);
    typedef unsigned int typedef_inside_function;
    pthread_join(id1, (void *) 0);
    pthread_join(id2, (void *) 0);
    if (x < 2) {
      __assert_fail("0", "simple_two.c", 24, __extension__ __PRETTY_FUNCTION__);
    } else if (x == 2) {
        x++;
    } else {
        x--;
    }
    for (int i = 0; i < 5; i++) {
        i++;
        for (int j = 0; j < 5; j++) {
            j++;
        }
    }
    int top;
    __VERIFIER_atomic_begin();
    if (top == 0) {
      __VERIFIER_atomic_end();
    } else {
      top = 1;
      __VERIFIER_atomic_end();
    }
    top = 42;
    pthread_t id3;
    pthread_create(&id3, (void *) 0, task1, (void *) 0);
}
