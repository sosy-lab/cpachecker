// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

struct s { int f; };

void f(struct s *s) {
	if ((*s).f != 1) {
ERROR:
		goto ERROR;
	}
}


void f2(int *p) {
	if ((*p) != 1) {
ERROR:
		goto ERROR;
	}
}

int main() {
	int *p = malloc(sizeof(int));
	(*p) = 1;
	f2(p);

	struct s *s = malloc(sizeof(struct s));
	s->f = 1;
	f(s);

	return 0;
}
