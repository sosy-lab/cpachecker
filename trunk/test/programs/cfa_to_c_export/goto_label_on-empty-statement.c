// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int a = 1;
  int c;
  int d;
  goto LABEL1;
  int b = 2;

LABEL2:;
  d = 4;
  return d;

LABEL1:;
  c = 3;
  goto LABEL2;
}
