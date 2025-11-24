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
    const int length = __VERIFIER_nondet_int();
    int array[length];

    for (int i = 0; i < length; ++i) {
        array[i] = 0;
    }

    const int random_element_index = __VERIFIER_nondet_int();

    if (random_element_index < 0 || random_element_index >= length) {
        return 0;
    }

    if (array[random_element_index] != 0) {
        __VERIFIER_error();
    }

    return 0;
}