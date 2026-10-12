// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int,
                           const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));

void reach_error() {
  __assert_fail("0", "simple_function_calls.c", 27, "reach_error");
}

extern int __VERIFIER_nondet_int();


int simple_function(int a) {
	return a + 1;
}

int main() {
	int x = __VERIFIER_nondet_int();
	simple_function(x);
	simple_function(x);
	if (x > 0) {
		int y = x - 1;
		if (y != x - 1) {
		  reach_error();
	  }
	}
	return 0;
}
