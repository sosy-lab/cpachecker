// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned long size_t;

void reach_error() {}
extern void abort(void);
extern int __VERIFIER_nondet_int(void);
extern unsigned long __VERIFIER_nondet_ulong(void);
extern void *calloc(size_t nmemb, size_t size);

// The flexible array member is zero, although the allocated type does not contain it.
struct entry {
  unsigned long key, value;
};
struct table {
  unsigned long size;
  struct entry slots[];
};

int main() {
  struct table *t = calloc(1, sizeof(struct table) + 2 * sizeof(struct entry));
  if (!t) {
    return 0;
  }
  t->size = 2;
  unsigned char *bytes = (unsigned char *)&t->slots[0];
  unsigned long i = __VERIFIER_nondet_ulong();
  if (i < 2 * sizeof(struct entry) && bytes[i] != 0) {
    reach_error();
  }
  return 0;
}
