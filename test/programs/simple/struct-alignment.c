// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

enum e { e };
struct list_head {
   struct list_head *next ;
   struct list_head *prev ;
};
struct s {
  enum e e;
  struct list_head list;
  int x;
};

void main() {
  struct s s = {0, 1};
  if (s.x) {
ERROR:
    return;
  }
}
