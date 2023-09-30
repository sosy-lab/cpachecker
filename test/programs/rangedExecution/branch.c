// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "branch.c", 7, __extension__ __PRETTY_FUNCTION__); })); }
extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint(void);

int main()
{
    unsigned int x = __VERIFIER_nondet_uint();
    unsigned int y = __VERIFIER_nondet_uint();

    if (x < y)
    { //int1
        unsigned int z = __VERIFIER_nondet_uint();
        if (x > z)
        {
            return 0;  //Leaf 1
        }
        else
        {
            if (x > 5)
            {
                return 0; //Leaf 2
            }
            else
            {
                if (y < z)
                {
                    ERROR: {reach_error();abort();} //Leaf 3
                }
                else
                {
                    return 0; //Leaf 4
                }
            }
        }
    }
    return 0;//Leaf 5
}