// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void reach_error(void) { abort(); }

char a[] = "x\"y\n";
char b[] = "ab" "cd";
char c[] = "\101\x42\0z";
char d[4] = "\\\t\x7f";

int main(void) {
  if (sizeof(a) != 5 || sizeof(b) != 5 || sizeof(c) != 5 || sizeof("\n") != 2) {
    reach_error();
  }
  if (a[1] != '"' || a[3] != '\n' || a[4] != 0) {
    reach_error();
  }
  if (c[0] != 'A' || c[1] != 'B' || c[2] != 0 || c[3] != 'z') {
    reach_error();
  }
  if (d[0] != '\\' || d[1] != '\t' || d[2] != 127 || d[3] != 0) {
    reach_error();
  }
  return 0;
}
