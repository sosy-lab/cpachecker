// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned int u32;

typedef union U {
  long pad;
  u32 a;
} U;

int main(void) {
  unsigned int x = 123u;
  U u = (U){ .a = (u32)x };

  if (u.a != (u32)x) goto error;
  return 0;

error:
  return -1;
}
