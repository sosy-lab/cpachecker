// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int a = 1;
  goto LABEL1;
  int b = 2;

LABEL1:;
  int c = 3;
  goto LABEL2;

LABEL2:
  int d = 4;
  return d;
}