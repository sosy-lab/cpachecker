// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  int x = 20;
  int y = 0;
  /*@ loop invariant x + y == 20;*/
  for (; y < 20; y++) {
    x--;
  }
  return x + y;
}
