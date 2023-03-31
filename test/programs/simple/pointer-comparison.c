// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  void *p = 0;
  void *q = -1;

  // GCC implements pointer comparisons as unsigned, so this branch is taken
  if (q > p) {
    goto ERROR;
  }
  return 0;

ERROR:
  return 1;
}
