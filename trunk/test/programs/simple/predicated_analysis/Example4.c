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
    int f;
    int x=0;
    if(y>0)
    {
	f=1;
    }
    else
    {
	f=0;
    }
    if(f>0)
    {
	x=y;
    }   
}
