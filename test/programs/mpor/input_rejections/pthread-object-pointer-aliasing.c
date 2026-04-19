// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <pthread.h>
pthread_mutex_t mutexA;
pthread_mutex_t mutexB;
void *task1(void *arg) {
    return;
}
int main() {
    pthread_mutex_init(&mutexA, (void *) 0);
    pthread_mutex_init(&mutexB, (void *) 0);

    pthread_mutex_t *mutex_ptr;
    mutex_ptr = &mutexA;
    mutex_ptr = &mutexB;
    pthread_mutex_lock(mutex_ptr);
    int x = 42;
    pthread_mutex_unlock(mutex_ptr);

    pthread_mutex_destroy(&mutexA);
    pthread_mutex_destroy(&mutexB);

    pthread_t id;
    pthread_create(&id, (void *) 0, task1, (void *) 0);
}
