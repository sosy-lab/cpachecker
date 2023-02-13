// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0


// In this struct, the offset of the last member (which has size 0)
// is not the same as the size of the whole struct.
struct s {
  int i;
  short s;
  short a[];
};

int main() {
  _Static_assert(__builtin_offsetof(struct s, a) == 6, "");
  _Static_assert(sizeof(struct s) == 8, "");

  if (__builtin_offsetof(struct s, a) == 6 && sizeof(struct s) == 8) {
ERROR:
    return 1;
  }
  return 0;
}
