// This file is part of DescribErr,
// a tool for finding error conditions:
// https://gitlab.com/sosy-lab/software/describerr
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer
//
// SPDX-License-Identifier: Apache-2.0


extern int __VERIFIER_nondet_int();

extern void __assert_fail (const char *__assertion, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert_perror_fail (int __errnum, const char *__file,
      unsigned int __line, const char *__function)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
extern void __assert (const char *__assertion, const char *__file, int __line)
     __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));

void reach_error() { ((void) sizeof ((0) ? 1 : 0), __extension__ ({ if (0) ; else __assert_fail ("0", "Problem10.c", 3, __extension__ __PRETTY_FUNCTION__); })); }

int main()
{
    // main i/o-loop
    while(1)
    {
        // read input
        int input = __VERIFIER_nondet_int();
        // operate eca engine
        if((input != 5) && (input != 1) && (input != 3) && (input != 2) && (input != 4))
          return -2;
        reach_error();
    }
}
