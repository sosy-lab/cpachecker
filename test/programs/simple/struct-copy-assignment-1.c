// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void* malloc(unsigned int);
struct s {
  int x;
  int y;
  char c[1];
};
void main() {
  struct s s1 = {1,2};
  struct s *p = malloc(sizeof(struct s));
  p->x = 0;
  p->y = 0;
  *p = s1;
  if (s1.x == p->x && s1.y == p->y) {
ERROR:
    return;
  }
}
