// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void){
  
  int a = 0;
  {
    __label__ testlabel;
    testlabel:

    if(a < 1) {
      a++;
      goto testlabel;
    }
  }

  {
   __label__ testlabel;

   int x = 2;
   if(x == 2) {
     x = 3;
     goto testlabel;
   }

   testlabel:

    if(a < 1) {
      a++;
      goto testlabel;
    }
  }

  {
   __label__ testlabel;
   testlabel:

    if(a < 1) {
      a++;
      goto testlabel;
    }
  }


  return 0;
}

