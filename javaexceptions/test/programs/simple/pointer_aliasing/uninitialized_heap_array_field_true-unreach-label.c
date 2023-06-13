// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long size_t;
struct S
{
	int a1;
	int a2;
};
extern void * malloc(size_t __size);

int __VERIFIER_nondet_int();

int arr[5];

int main()
{
	struct S* s1 = (struct S*) malloc(sizeof(struct S));
	arr[s1->a2] = s1->a1;
	if (s1->a2 == 1 && s1->a1 != arr[1]) {
ERROR:
		return 1;
	}
	return 0;
}
