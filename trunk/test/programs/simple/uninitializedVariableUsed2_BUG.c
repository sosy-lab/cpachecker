// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __fail(void) {
   ERROR: goto ERROR;
}

int main(void) {
   int a,x;
   if(a>0) {
       x=2;
   } else {
       x=3;
   }
   if(x==2) __fail();
   return a;
}
