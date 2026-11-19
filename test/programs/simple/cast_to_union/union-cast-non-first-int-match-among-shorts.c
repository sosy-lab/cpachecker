// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef union  {
  short a;
  short b;
  int x; //target of cast not first member of union
  short c;
} uv;

int main() {
  uv value = (uv) 100000; //100_000 is bigger than the biggest possible short

  if (value.x != 100000) {
    goto error;
  }
  return 0;
error:
  return -1;
}
