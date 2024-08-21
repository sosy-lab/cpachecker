// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// #include <assert.h>

struct RR
{
	int state;
};

typedef struct RR rr;

rr * __undefrr();
void * __undefined_pointer();
int    __undefined_int();
