// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(){}

int main() {
  int i;
  int x = 0;
  for (i = 0; i < 10; i++) {
    x++;
  }
  if (11 == x) {
    reach_error();
  }
}
