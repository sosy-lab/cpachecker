// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

enum E { A = 1, B = 2 };

typedef union U {
  int i;
  enum E e;
} U;

int main(void) {
  enum E x = B;
  U u = (U) x;   // must choose e

  if (u.e != B) goto error;
  return 0;

error:
  return -1;
}
