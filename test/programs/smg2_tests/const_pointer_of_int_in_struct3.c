// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct Foo {
  int temp;
  int *p;
};

int main() {
  int a = 0;
  struct Foo f;
  f.p = &a;
  *f.p = 2;

  return 0;
}
