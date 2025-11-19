// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  struct {
    struct {
      struct {
        struct {
          int c;
        };
        struct {
          int d;
        };
      } b[4];
    } a;
    struct {
      struct {
        int e;
      };
    };
  } s = {.a.b[0U + 1].d = 2, {.c = 3, .d = 4}, 10, 11, {.e = 15}};

  if (s.a.b[1].d != 2) {
    goto ERROR;
  }
  if (s.a.b[2].c != 3) {
    goto ERROR;
  }
  if (s.a.b[2].d != 4) {
    goto ERROR;
  }
  if (s.a.b[3].c != 10) {
    goto ERROR;
  }
  if (s.a.b[3].d != 11) {
    goto ERROR;
  }
  if (s.e != 15) {
    goto ERROR;
  }

  return 0;
ERROR:
  return -1;
}
