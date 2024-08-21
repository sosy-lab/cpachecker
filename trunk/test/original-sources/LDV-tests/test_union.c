// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <assert.h>

#ifdef BLAST_AUTO_1
int VERDICT_SAFE;
int CURRENTLY_UNSAFE;
#else
int VERDICT_SAFE;
int CURRENTLY_SAFE;
#endif


union A {
	int list;
	int l2;
	char * str;
};

int main(void) {
	union A x;
	x.list = 0;
#ifdef BLAST_AUTO_1
	assert(x.l2 == 0);
#else
	assert(x.list==0);
#endif
}
