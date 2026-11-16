// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(void);
unsigned long size_packed(void);

struct S {
  char c;
  int i;
};

int main(void) {
  if (size_packed() != 5) {
    reach_error();
  }
  if (sizeof(struct S) != 8) {
    reach_error();
  }
  return 0;
}
