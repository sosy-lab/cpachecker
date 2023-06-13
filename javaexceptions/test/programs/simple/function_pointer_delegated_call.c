// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int foo(void bar(int), int i) {
	bar(i);
	return ++i;
}

long x = 0;

void baz(int i) {
	x += i;
}

int main(int args, char** argv) {
	void (*p)(int) = &baz;
	long long i = 0;

	for(; i < 10; i++) {
		baz(foo(*p, i));
	}

	if(x == 100) {
ERROR:
		return 1;
	}

	return 0;
}
