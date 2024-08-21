// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

//ABE  invariant not feasible to implement
int f(int var1, int var2)
{
 int y = var1+var2;
 y = y+2;
 return y;
}

int main()
{
  int a=0;
  int x;
  
  if(x<0){
    x=0;
    a = f(x, a);
  }
  else
  {
    a = f(x,a);
  }
  a++;
  if(a<3){
    ERROR: return -1;
  }

  return 0;
}
