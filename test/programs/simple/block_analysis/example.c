// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int x(int y);
void reach_error(){}
void main() {
  
  int a = x(1);
  int b = x(2);
  int c = x(1);

  if (a != c) reach_error();

}
