// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Arrays with the same string initializer are distinct objects.

extern void abort(void);
void reach_error(void) { abort(); }

const char a[] = "ab";
const char b[] = "ab";
const void *p[2] = {0, b};

int main(void) {
  const void *q = a;
  if (q == (const void *)a && q != (const void *)b && a[1] == 'b') {
    reach_error();
  }
  return 0;
}
