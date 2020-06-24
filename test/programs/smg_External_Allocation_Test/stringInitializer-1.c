// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int a[] = {1, 2, 3};
  char b[] = "ab";
  char c;
  int d;
  c = b[3];
  d = a[2];
  return 0;
}
