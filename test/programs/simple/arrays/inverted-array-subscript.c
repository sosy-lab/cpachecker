// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int a = 0;
  int *p = &a;
  if (p[a] != a[p]) {
ERROR:
    return 1;
  }
  return 0;
}
