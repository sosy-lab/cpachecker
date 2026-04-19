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
pthread_mutex_t mutexA;
pthread_mutex_t mutexB;
pthread_mutex_t mutexC;
struct __anonstruct_PQUEUE_63 {
    int occupied ;
    pthread_mutex_t inner_mutex ;
};
typedef struct __anonstruct_PQUEUE_63 PQUEUE;
struct __anonstruct_PQUEUE_64 {
    int occupied ;
    pthread_mutex_t *inner_mutex_pointer ;
};
typedef struct __anonstruct_PQUEUE_64 PQUEUE_PTR;
PQUEUE struct_with_mutex;
PQUEUE another_struct_with_mutex;
PQUEUE_PTR struct_with_mutex_ptr;
PQUEUE_PTR yet_another_struct_with_mutex_ptr;
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
    do
        y = 20;
    while (__VERIFIER_nondet_int());
    label:
    y = 10;
    if (__VERIFIER_nondet_int()) {
        goto label;
    }
}
void local_increment(int number) {
    number += 1;
}
void pass_parameter(int number) {
    number += 1;
    local_increment(number);
    number += 1;
}
void unused_parameter(int number) {
    if (x == 64) {
        x = 32;
    }
}
void pass_mutex(pthread_mutex_t the_mutex) {
    pthread_mutex_lock(&the_mutex);
    printk("another print");
    pthread_mutex_unlock(&the_mutex);
}
void pass_mutex_pointer(pthread_mutex_t *the_mutex_pointer) {
    pthread_mutex_lock(the_mutex_pointer);
    printk("mutex pointer sandwich");
    pthread_mutex_unlock(the_mutex_pointer);
}
void pass_struct_ptr(PQUEUE * a_struct_ptr) {
    pthread_mutex_lock(a_struct_ptr->inner_mutex);
    printk("look!", "what happens here?");
    pthread_mutex_unlock(a_struct_ptr->inner_mutex);

    pass_struct_ptr_again(a_struct_ptr);
}
void pass_struct_ptr_again(PQUEUE * a_struct_ptr_again) {
    pthread_mutex_lock(a_struct_ptr_again->inner_mutex);
    printk("and what about this?");
    pthread_mutex_unlock(a_struct_ptr_again->inner_mutex);
}
const int global_const = 0;
int main() {
    const int local_const = 7;
    if (x != 1) {
      __assert_fail("0", "simple_two.c", 24, __extension__ __PRETTY_FUNCTION__);
    }
    x = 0;
    local_increment(x);
    int local_non_const = 3;

    pthread_mutex_init(&mutexA, (void *) 0);
    pthread_mutex_init(&mutexB, (void *) 0);

    // a ghost variable should still be created for the mutex, even if it is not explicitly accessed
    pthread_mutex_t *mutexC_ptr = &mutexC;
    pthread_mutex_init(mutexC_ptr, (void *) 0);

    pthread_mutex_init(&struct_with_mutex.inner_mutex, (void *) 0);
    pthread_mutex_init(&another_struct_with_mutex.inner_mutex, (void *) 0);

    // mutex aliasing must be handled
    pthread_mutex_t *mutex_ptr;
    mutex_ptr = &mutexA;
    mutex_ptr = &mutexB;
    mutex_ptr = &struct_with_mutex.inner_mutex;
    pthread_mutex_lock(mutex_ptr);
    x = 42;
    pthread_mutex_unlock(mutex_ptr);

    // this is undefined behavior in the pthread standard (assign mutex and pass by value)
    /* pthread_mutex_t uninit_mutex;
    uninit_mutex = mutexA;
    pass_mutex(mutexA); */

    // passing a pointer to a mutex is fine and not undefined behavior
    pass_mutex_pointer(mutex_ptr);

    PQUEUE *ptr_to_struct;
    if (x == 123456789) {
        ptr_to_struct = &struct_with_mutex;
    } else {
        ptr_to_struct = &another_struct_with_mutex;
    }
    pthread_mutex_lock(ptr_to_struct->inner_mutex);

    pthread_mutex_t *another_mutex_ptr;
    another_mutex_ptr = &ptr_to_struct->inner_mutex;

    struct_with_mutex_ptr.inner_mutex_pointer = &mutexA;
    yet_another_struct_with_mutex_ptr.inner_mutex_pointer = &mutexB;

    PQUEUE_PTR *ptr_to_struct_with_ptr;
    if (x == 987654321) {
        ptr_to_struct_with_ptr = &struct_with_mutex_ptr;
    } else {
        ptr_to_struct_with_ptr = &yet_another_struct_with_mutex_ptr;
    }
    if (x == 192837465) {
        ptr_to_struct_with_ptr->inner_mutex_pointer = &mutexC;
    }
    pass_mutex_pointer(ptr_to_struct_with_ptr->inner_mutex_pointer);

    pthread_mutex_t *yet_another_mutex_ptr;
    yet_another_mutex_ptr = struct_with_mutex_ptr.inner_mutex_pointer;
    pthread_mutex_lock(yet_another_mutex_ptr);
    pthread_mutex_unlock(yet_another_mutex_ptr);
    pass_mutex_pointer(yet_another_mutex_ptr);

    PQUEUE *another_ptr_to_struct;
    another_ptr_to_struct = &struct_with_mutex;
    pass_struct_ptr(another_ptr_to_struct);

    pthread_mutex_destroy(&mutexA);
    pthread_mutex_destroy(&mutexB);
    pthread_mutex_destroy(&struct_with_mutex.inner_mutex);
    pthread_mutex_destroy(&another_struct_with_mutex.inner_mutex);

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
    pass_parameter(x);
    unused_parameter(x);
    while (1) {
        local_increment(x);
    }
    while (__VERIFIER_nondet_int()) {
        x++;
        x *= 2;
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
