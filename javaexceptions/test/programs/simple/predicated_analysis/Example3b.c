// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main()
{
    int x=0;
    x=x+1;
    int i=0;
    while(1)
    {
	if(i==1)
	{
	    x=x+1;
	    i=0;
	}
	else
	{
	    x=x-1;
	    i=1;
	}
    }
}
