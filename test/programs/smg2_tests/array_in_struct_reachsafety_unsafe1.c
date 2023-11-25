// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Currently fails due to a bug in the MachineModel, returning the s.a[4] offset for s.c
int main() {
  int size = 5;
  struct s {
    char a[size];
    char c;
  } s;

  s.a[4] = 3;
  s.c = 2;
  if (s.a[4] == 2 || s.c == 3) {
    return 0;
  }

ERROR:
  return 1;
}
