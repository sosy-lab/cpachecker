// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){
  int x;

  if(a<0){
    x=1;
  }
  else{
    x=2;
  }

  if(x<0){
    ERROR: return -1;
  }

  return 0;
}
