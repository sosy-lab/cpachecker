// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#ifdef BLAST_AUTO_1
  #include <assert.h>
#else
  void assert(int i)
  {
	if (i == 0)
	{
		ERROR: goto ERROR;
	}
  }
#endif

/*
We've got an exception NoNewPredicatesException on program oomInt.c.
*/
#ifdef BLAST_AUTO_1
int VERDICT_SAFE;
int CURRENTLY_SAFE;
#else
int VERDICT_SAFE;
int CURRENTLY_UNKNOWN;
#endif

int abs_int(int i)
{
	if (i < 0) 
	{
//		assert( i < 0);
//		assert(-i > 0);
		return -i;
	}
	else	return +i;
}
int p = 0;
void firstFunction()
{
	p = abs_int(-3);
	assert(p >= 0);
}

void main()
{
	firstFunction();
}
