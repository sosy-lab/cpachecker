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
  int i = 0;
  x = voli(x);
  x = aux(x);
  }

int voli(int x) {
  int v = __VERIFIER_nondet_uint();
  int i = 2;
  int in[3];
  in[0] = 0;
  in[i] = 2;
  x = 10;
  x = x + 1;
  int y = __VERIFIER_nondet_uint();
  x = x + 2;  
  if (y == 1) {
    x = 0;
  } else {
    x = 1;
  }
  return x;
}

int aux(int z) {
  if (z == 1) {
    z = 2;
  } else {
    z =3;
  }
  return z;
}


