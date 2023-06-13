// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error();
void main() {
  int i = 0;
  int j = 0;
  FIRST:
  j = j + 1;
  SECOND:
  if (j < 4) {
    goto FIRST;
  } else {
    i = i + 1;
    if (i > 6) {
      ERROR:
      goto ERROR;
    } else if (i == 6) {
      return;
    } else {
      goto SECOND;
    }
  }
}
