// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail(const char *assertion, const char *file,
                          unsigned int line, const char *function);

int main() {
    int size = 2000000;

    int B[size];

    int x = 0;
    while (x < size) {
        B[x] = -5;
        x = x + 1;
    }

    if ((B[30]!=-5)){
        __assert_fail("0", "array_init2.c", 24, "main");
    }
}