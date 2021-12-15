// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

long undef_func();

struct test {
  long res;
};

int main() {
  struct test t;
  t.res = undef_func();
  return t.res;
}