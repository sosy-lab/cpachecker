// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int i;
int a;

int main() {
  i = 0;
  a = 0;

  while(1) {
    if(i== 20){
       goto LOOPEND;
    }
    else{
       i++;
       a++;
    }
  }

  LOOPEND:

  if (a != 20) {
     goto ERROR;
  }
  else {}
  
  return (0);
  ERROR:
  return (-1);
}

