// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {

  int cards[] = {[10 ... 15] = 1, 2, [1 ... 5] = 3, 4};
  if (sizeof(cards) != 17*sizeof(int)) {
    ERROR: return 1;
  }
  return 0;
}
