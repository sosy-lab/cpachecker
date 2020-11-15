// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int i;
int a;

void main() {
  i = 0;
  a = 0;

  while(1) {
    if(i== 5){
       break;
    }
    else{
       i++;
       a++;
    }
  }

  LOOPEND:

  if (a != 5) {
     goto ERROR;
  }
  else {}
  
  return;
  ERROR:
  return;
}

