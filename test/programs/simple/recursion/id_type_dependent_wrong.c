// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

unsigned int id(unsigned int x) {
  if (x==0) return 0;
  unsigned int ret = id(x - 1) + 1;
  if (ret > 100) return 100;
  return ret;
}

int main(void) {
  int result = id(0);
  if (result == 0) {
    ERROR: {reach_error();abort();}
  }
}