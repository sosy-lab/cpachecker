// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

int main() {
  int i = 1;
  int p = 1;

      if (i != 1) goto ERROR;
      if (p != 1) goto ERROR;
      return 0;
      ERROR:
        reach_error();
        return 1;
}
