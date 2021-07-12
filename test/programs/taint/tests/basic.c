// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int getchar();
extern void printf(char*, int);

int main(void){
	int a, b, c, d;						// T(*) = U
	a = getchar();						// T(a) = T
	b = 10;                 			// T(b) = U
	c = a;                 				// T(c) = T
	d = b;								// T(d) = U
	c = 2;								// T(c) = U
	printf("%d", a);					// T(a) = T
	printf("%d", a);					// T(b) = U
	return 0;
}
