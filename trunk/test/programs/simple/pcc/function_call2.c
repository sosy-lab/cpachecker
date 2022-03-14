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

void test1(){
  int z=0;
}

int test2(){
  return 0;
}

void test3(int a){
 int w=a+2;
}

int main()
{
  int x = 2;
  test1();
  test2();
  if(test2())
  {
    goto ERROR;
  }
  test3(x);
  compute_square(x); 
  x = compute_square(x) + 2;
  if(x!=7)
  {
     goto ERROR;
  }
  return 0;

ERROR: return -1;
}
