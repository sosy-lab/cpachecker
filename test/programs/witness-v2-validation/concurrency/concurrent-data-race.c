// This file is part of the SV-Witnesses repository as regression test:
// https://gitlab.com/sosy-lab/benchmarking/sv-witnesses
//
// SPDX-FileCopyrightText: 2025 The SV-Witnesses Community
//
// SPDX-License-Identifier: Apache-2.0

// Concurrent data race: two threads write to x with no synchronization.
// Property: G ! data-race

#include <pthread.h>

int x = 0;

void* t1_func(void* arg) {
    x = 1;
    return NULL;
}

void* t2_func(void* arg) {
    x = 2;
    return NULL;
}

int main(void) {
    pthread_t ta, tb;
    pthread_create(&ta, NULL, t1_func, NULL);
    pthread_create(&tb, NULL, t2_func, NULL);
    pthread_join(ta, NULL);
    pthread_join(tb, NULL);
    return 0;
}
