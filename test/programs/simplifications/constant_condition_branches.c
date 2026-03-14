// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int x = 0;

  if (1) {
    x = 7;
  } else {
    goto ERROR;
  }

  while (0) {
    goto ERROR;
  }

  for (; 0;) {
    goto ERROR;
  }

  if (x != 7) goto ERROR;

  return 0;
ERROR:
  return 1;
}
