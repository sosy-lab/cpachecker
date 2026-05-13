// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int rand_int();

int abs(int x){
  if(x >= 0){
    return x;
  }else{
    return abs(x * -1);
  }
}

int main(){

  int x = rand_int();

  int z = abs(x);

  if(z < 0){
    ERROR: return 0;
  }

  return 1;
}

