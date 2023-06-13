// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

static const unsigned char g_4 = 1;
static unsigned long long g_139 = 2;

static unsigned char func_2(const unsigned char p_3);
static const unsigned char func_5(unsigned char p_6);
static int func_12();

static const unsigned char func_5(unsigned char p_6) {
  while (g_139 > g_4)
    ;
ERROR:
  goto ERROR;
  return;
}

static int func_12() {
  g_139 = 0;
  return 0;
}

int main(int argc, char *argv[]) {
  if (func_5(g_4 && func_12()))
    ;
  return 0;
}
