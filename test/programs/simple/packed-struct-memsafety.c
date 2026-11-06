// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Accesses to members of packed structs must use the packed layout (#411).

extern void *malloc(unsigned long);
extern void free(void *);

struct __attribute__((packed)) S {
  char c;
  int i;
};

int main() {
  struct S *p = malloc(5);
  if (p) {
    p->c = 1;
    p->i = 2;
    free(p);
  }
  return 0;
}
