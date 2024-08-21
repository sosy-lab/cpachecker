// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int compute_square(int y)
{
   return y+3;
}

int main()
{
  int x = 2;
  x= compute_square(x)+ 2; 
  x = compute_square(x) + 2;
  if(x!=12)
  {
     goto ERROR;
  }
  return 0;

ERROR: return -1;
}
