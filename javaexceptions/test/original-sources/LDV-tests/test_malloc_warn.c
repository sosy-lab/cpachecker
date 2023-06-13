// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

struct miniStruct {
	int a;
	int b;
};

int
main(int argc, char* argv[]) {
	struct miniStruct *minis;
	minis = malloc(sizeof(minis));
	assert(sizeof(minis) == sizeof(struct minis));
	return 0;
}
