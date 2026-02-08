// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef union {
  union{
    int i1;
    double d1;
  } ;
  int i2;
} outer;

int main(){
  double d = 0.3;
  outer o = (outer) d;
}