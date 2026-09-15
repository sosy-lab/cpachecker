// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() { }
struct S {
  int z;
  struct {
    int a;
  };
  struct {
    int b;
  };
  struct {
    int c;
  };
  struct {
    int d;
    struct {
      int f;
    } innerNamed;
  };
  struct {
    struct {
      int e;
    };
  } named;
};

int main() {
  struct S x = {{}, {}, 5, .d = 10, {12}, {.e = 15}};

  if (x.b != 5) {
    goto ERROR;
  }
  if (x.d != 10) {
    goto ERROR;
  }
  if (x.innerNamed.f != 12) {
    goto ERROR;
  }
  if (x.named.e != 15) {
    goto ERROR;
  }
  return 0;

ERROR:
  reach_error();
  return 1;
}
