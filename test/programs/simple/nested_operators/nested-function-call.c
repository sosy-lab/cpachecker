// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void err() {
ERROR:
  goto ERROR;
}

static int func_32(int _);
static int func_38();

static int func_32(int _) {
  while (1)
    ;
  return 0;
}

static int func_38() {
  err();
  return 0;
}

int main(void) {
  if (func_32((0, func_38())))
    ;
  return 0;
}
