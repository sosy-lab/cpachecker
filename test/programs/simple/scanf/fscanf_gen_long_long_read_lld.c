// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <stdio.h>

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "fscanf_gen_long_long_read_lld.c", 3, "reach_error"); }

int main() {
long long i = 0;

fscanf(stdin,"%lld", &i);

if(i > 0) {
    reach_error();
}

return 0;
}