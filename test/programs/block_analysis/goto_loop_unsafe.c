// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

  int x = 5;
  if (x != 5) {
    while (x != 0) {
    LOOP:
      x--;
    }
    goto ERROR;
  }
  goto LOOP;
ERROR:
  return 1;
}
