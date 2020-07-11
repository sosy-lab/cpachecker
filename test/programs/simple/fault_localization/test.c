// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
/*	int x = __VERIFIER_nondet_int();
	if (x <= 0)
		goto EXIT;
	int answer = 0;
	while (1) {
		x -= 2;
		if (x < 0) {
			x += 2;
			if (x == 1) {
				answer = 1;	
			}
			break;
		}
	}

	if (x % 2 == 0 && answer == 0) {
		goto ERROR;
	}

int x = 0;
x++;
if (x == 1) {
 int i = 0;
 int j = 2;
  while(i != j) i++;
  goto ERROR;
}*/
int a = 1;
int i;
for (i = 0; i < 5; i++) a++;
if (i != 5 || a == 6) goto ERROR;	
EXIT:
	return 0;
ERROR:
	return 1;
}
