// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long unsigned int size_t;

void reach_error() {}
extern void __VERIFIER_nondet_memory(void *ptr, size_t size);
extern void *malloc(size_t size);

struct S {
  int a;
  int b[];
};

int main() {
  // room for 'a' plus 3 elements of the flexible array member 'b'
  struct S *sp = malloc(16);
  if (sp == 0) {
    return 0;
  }
  sp->a = 0;
  sp->b[0] = 0;
  sp->b[1] = 0;
  sp->b[2] = 0;
  __VERIFIER_nondet_memory(sp, 16);
  if (sp->a == 1 && sp->b[0] == 2 && sp->b[1] == 3 && sp->b[2] == 4) {
    // branch may be entered
    reach_error();
  }
}
