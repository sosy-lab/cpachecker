// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
#include <stdio.h>

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "fscanf_gen_unsigned_char_read_hhx.c", 3, "reach_error"); }

int main() {
unsigned char i = 0;

fscanf(stdin,"%hhx", &i);

if(i > 0) {
    reach_error();
}

return 0;
}