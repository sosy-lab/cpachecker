// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
int a;
int i,c;
int main() {
	a = __VERIFIER_nondet_int();
	i = 0;
	c = 0;
	while(i < 500) {
		c = c + i;
		i = i + 1;
	}
	if(a <= 0) {
		ERROR: goto ERROR;
	}else{
		return 0;
	}
	return 1;
}
