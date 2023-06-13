// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
system is unsafe, but blast reports it is safe
*/
int VERDICT_UNSAFE;
int CURRENTLY_SAFE;

void f(void g(int)) {
	g(1);
}

void h(int i) {
	if(i==1) {
		ERROR: goto ERROR;
	} else {
		//ok
	}
}
int main(void) {
	f(h);
	//h(0);
	return 0;
}
