// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_uint();

int main()
{
  int x = foo();
  
  if (x>0) 
  {
    x = x -1;
  }
  
  return x;
}

int foo() 
{
  int a[1];
  a[0] = __VERIFIER_nondet_uint();

  if (a[0] != 1) 
  {
    a[0] = 0;
  } 
 
  return a[0];
}


