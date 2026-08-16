// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef union U {
  int *p;
  const int *cp;
  long dummy;
} U;

int main(void) {
  const int *x = 0;
  U u = (U) x;      // must choose cp

  if (u.cp != x) goto error;
  return 0;

error:
  return -1;
}
