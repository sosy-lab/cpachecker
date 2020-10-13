// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/* Complex lvalue assignment
 */

#include <assert.h>

int VERDICT_SAFE;

typedef struct Toplev {
	int a;
	struct Inner {
		int b;
		struct Innermost{
			int c;
		} *y;
	} *x;
} Stuff;

int main()
{
	struct Innermost im = {3};
	struct Inner inner = {2, &im};
	struct Toplev good = { 1, &inner};
	good.x->y->c = 4;
	assert (good.x->y->c == 4);
	return 0;
}






