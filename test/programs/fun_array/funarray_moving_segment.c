/*
 * This file is part of CPAchecker,
 * a tool for configurable software verification:
 * https://cpachecker.sosy-lab.org
 *
 * SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);

int main(void) {
    const int array_length = __VERIFIER_nondet_int();
    const int segment_length = __VERIFIER_nondet_int();

    if (segment_length > array_length) {
        return 0;
    }

    int array[array_length];

    for (int i = 0; i < array_length; ++i) {
        if (i < segment_length) {
            array[i] = 1;
        } else {
            array[i] = 0;
        }
    }

    int segment_start = 0;

    while (segment_start + segment_length < array_length) {
        array[segment_start] = 0;
        array[segment_start + segment_length] = 1;
        segment_start++;
    }

    for (int i = 0; i < segment_start; i++) {
        if (array[i] != 0) {
            __VERIFIER_error();
        }
    }
    for (int i = segment_start; i < array_length; i++) {
        if (array[i] != 1) {
            __VERIFIER_error();
        }
    }

    return 0;
}