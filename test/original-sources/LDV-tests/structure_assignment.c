// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/* Structure assignment test
 */

#include <assert.h>

int VERDICT_SAFE;

typedef struct Stuff {
	int a;
	int b;
} Stuff;

int main()
{
	Stuff good = {1,2};
	Stuff bad;
	bad = good;
	assert (bad.b == 2);
	return 0;
}





