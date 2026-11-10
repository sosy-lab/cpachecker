// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

extern void *calloc(unsigned long count, unsigned long size);
extern void free(void *ptr);

struct Numbers {
  char c[3];
  short s[3];
  int i[3];
  long l[3];
  long long ll[3];
};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// calloc's all-zero bytes represent zero for every tested integer element; padding is not inspected.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct Numbers *p = calloc(1UL, sizeof *p);
  if (p == 0) {
    return 0;
  }

  __VERIFIER_assert(p->c[0] == 0);
  __VERIFIER_assert(p->c[1] == 0);
  __VERIFIER_assert(p->c[2] == 0);
  __VERIFIER_assert(p->s[0] == 0);
  __VERIFIER_assert(p->s[1] == 0);
  __VERIFIER_assert(p->s[2] == 0);
  __VERIFIER_assert(p->i[0] == 0);
  __VERIFIER_assert(p->i[1] == 0);
  __VERIFIER_assert(p->i[2] == 0);
  __VERIFIER_assert(p->l[0] == 0);
  __VERIFIER_assert(p->l[1] == 0);
  __VERIFIER_assert(p->l[2] == 0);
  __VERIFIER_assert(p->ll[0] == 0);
  __VERIFIER_assert(p->ll[1] == 0);
  __VERIFIER_assert(p->ll[2] == 0);

  free(p);

  return 0;
}
