// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

# 1 "files/callfpointer.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "files/callfpointer.c"





void f(void g(int)) {
 g(1);
}

void h(int i) {
 if(i==1) {
  ERROR: goto ERROR;
 } else {

 }
}

void z(int i) {}

int main(void) {
 void (*z)(int) = &h;

 // in the next line, z references the local variable
 f(z);

 return 0;
}
