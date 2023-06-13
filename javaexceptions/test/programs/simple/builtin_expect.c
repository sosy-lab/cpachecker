// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
	int i = 5;

	// This is the GCC built-in function __builtin_expect(exp, c)
	// that behaves like (exp == c).
	// http://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-g_t_005f_005fbuiltin_005fexpect-3345
	if (__builtin_expect(i, 5)) {
		return 0;
	} else {
ERROR:
		return 1;
	}
}
