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
    int x=5;
    if(y>1)
    {
	x=y;
    }
    if(x==y)
    {
	x=x-1;
    }
}
