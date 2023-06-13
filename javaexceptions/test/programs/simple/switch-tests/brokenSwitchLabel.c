// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main() {
 
 int N = 5;
 int a[N];
 int i;
 if (i) goto LABEL_TEST;
 switch(2) {
 
 case 0:
   for(i=0; i < N; ++i) { LABEL_TEST: a[i] = i;}
  break;
  
 case 1:
   for(i=(N-1); i >= 0; --i) a[i] = N-1-i;
  break;
  
 case 2:
   for(i=0; i < N; ++i) a[i] = i;
  break;
 }
}

