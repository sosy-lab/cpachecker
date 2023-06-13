// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main()
{
    int y;
    int i=0;
    int x = 0;
    if(y<0)
    {
	y=0;
    }else{
	y=5;
    }
    x=y;
    x=x+1;
    while(1)
    {
	if(i==1)
	{
	    x = x+1;
	    i=0;
	}
	else
	{
	    x = x-1;
	    i=1;
	}
    }
}
