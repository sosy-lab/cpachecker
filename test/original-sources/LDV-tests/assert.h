// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __blast_assert()
{
	ERROR: goto ERROR;
}

//#define assert(cond) do {if (!cond) __blast_assert();} while(0);
# define assert(expr)							\
  ((expr)								\
   ? (0) \
   : __blast_assert ())


