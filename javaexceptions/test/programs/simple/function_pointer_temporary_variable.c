// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f() {
  return 0;
}

void error() {
ERROR:
  return;
}

int main() {
  void (*fp)(void );
  fp = f() ? (void *)0 : error;
  fp();
  return 0;
}
