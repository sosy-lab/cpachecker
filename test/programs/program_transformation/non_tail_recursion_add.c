// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

unsigned int rand_unsigned_int();

unsigned int add(unsigned int x, unsigned int y){
  if(y == 0){
    return x;
  }else{
    return 1 + add(x, y-1);
  }
}

int main(){

  unsigned int x = rand_unsigned_int();
  unsigned int y = rand_unsigned_int();

  unsigned int z = add(x, y);

  if(z == (x + y)){
    return 1;
  }else{
    ERROR: return 0;
  }

}

