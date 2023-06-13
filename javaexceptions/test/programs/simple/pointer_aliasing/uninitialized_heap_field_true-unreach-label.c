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

int main()
{
	struct S* s1 = (struct S*) malloc(sizeof(struct S));
	s1->a2 = s1->a1;
	if (s1->a1 != s1->a2) {
ERROR:
		return 1;
	}
	return 0;
}
