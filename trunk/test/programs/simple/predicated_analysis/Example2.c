// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main()
{
    int a;
    int b;
    int y;
    int i;
    int x=0;
    if(a<0)
    {
	x=-a;
    }
    else
    {
    	x=a;
    }
    y=b*a;
    if(y>x)
    {
	i=0;
    }
    else
    {
	i=1;
    }
    while(b>0)
    {
	if(i==0)
	{
	    x=x+y;
	}
    }
}
