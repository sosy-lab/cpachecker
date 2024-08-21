// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main() {
  int i = 1;
  if (!i) {
    int *p = &i;
    (*p)++;
  } else {
  }
  if (i != 1) {
ERROR:
    return;
  }
}
