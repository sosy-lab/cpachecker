// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_uint();
int main() {
  int x = __VERIFIER_nondet_uint();
  int y = __VERIFIER_nondet_uint();    
  int i = __VERIFIER_nondet_uint();
  int in[3];
  in[0] = 10;  
  in[1] = y;
  in[i] = 5;
  int z0 = in[0];
  int z1 = in[1];
  int zi = in[i];
  
  if (y == 2) { 
    x = 1;
  } else {
    x = 0;
  }
  if (x == 1) {
    x = 10;
  } else {
    x = -10;
  }
  return x;
}


