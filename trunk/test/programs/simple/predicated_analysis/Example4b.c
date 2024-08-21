// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main()
{
    int f;
    int y;
    int x=0;
    if(f>0)
    {
	x=1;
	y=1;
    }
    else
    {
	x=2;
	y=3;
    }
    if(y==2)
    {
	x=x-2;
    }   
}
