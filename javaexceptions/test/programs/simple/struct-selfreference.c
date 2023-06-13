// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct list_head {
	struct list_head *next;
	struct list_head *prev;
};

struct list_head l1, l2;

int main(void)
{
	struct list_head *h;
	int condition;
	h = condition ? &l1 : &l2; 
	ERROR:
		goto ERROR;
	return 0;
}
