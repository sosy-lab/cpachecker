// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void reach_error() { __assert_fail("0", "return_true.c", 3, "reach_error"); }


int 
add(int a, int b) 
{
    return a + b;
}


int 
main() 
{
    int x = 31;
    int y = 11;
    if (add(x, y) != 42) {
        reach_error();
    }

}