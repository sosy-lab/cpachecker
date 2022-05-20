// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// depending on when it is noticed that path not feasible ( cpa.predicate.satCheck>0<threshold), adjustable LBE  invariant not feasible to implement

int main()
{
  int a=0;
  int x=5;
  int y=4;
  
  while(x>0){
    if(x-y==0){
      a++;
      y=y-2;
    }
    a++;
    x--;
  }
  if(a!=7){
    ERROR: return -1;
  }

  return 0;
}
