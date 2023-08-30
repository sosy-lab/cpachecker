// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main()
{
    int a = 3; 
    int b = 9;
    int c = 7; 
    int d = 2; 
    if ((a + b) * c / d != 42) { ERROR: ; }

}