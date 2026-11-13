// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A string literal in braces initializes a char array like the plain string literal
// (C11 § 6.7.9 (14), pattern from gnulib's regex in coreutils).

extern void abort(void);
void reach_error(void) { abort(); }

static const char g[5] = {"abc"};
char u[] = {"xyz"};

int main(void) {
  char a[] = {"abc"};
  char b[5] = {"ab"};
  unsigned char c[3] = {"xy"};
  if (g[1] != 'b' || g[3] != 0 || g[4] != 0) {
    reach_error();
  }
  if (sizeof(u) != 4 || u[2] != 'z' || u[3] != 0) {
    reach_error();
  }
  if (sizeof(a) != 4 || a[0] != 'a' || a[3] != 0) {
    reach_error();
  }
  if (b[1] != 'b' || b[2] != 0 || b[4] != 0) {
    reach_error();
  }
  if (c[1] != 'y' || c[2] != 0) {
    reach_error();
  }
  return 0;
}
