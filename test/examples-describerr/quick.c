// This file is part of DescribErr,
// a tool for finding error conditions:
// https://gitlab.com/sosy-lab/software/describerr
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "const_1-2.c", 3, "reach_error"); }

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

int main(void) {
  unsigned int x = 1;
  unsigned int y = 0;

  while (y < 1024) {
    x = 0;
    y++;
  }

  __VERIFIER_assert(x == 1);
}
