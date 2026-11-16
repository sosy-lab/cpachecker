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

// More zeroed blocks than the encoding keeps facts for.
#define ALLOC(p)            \
  unsigned char *p = calloc(4, 1); \
  if (!p) {                 \
    return 0;               \
  }

int main() {
  ALLOC(p1) ALLOC(p2) ALLOC(p3) ALLOC(p4) ALLOC(p5) ALLOC(p6) ALLOC(p7) ALLOC(p8)
  ALLOC(p9) ALLOC(p10) ALLOC(p11) ALLOC(p12) ALLOC(p13) ALLOC(p14) ALLOC(p15)
  ALLOC(p16) ALLOC(p17) ALLOC(p18) ALLOC(p19) ALLOC(p20) ALLOC(p21) ALLOC(p22)
  if (*(int *)p22 != 0) {
    reach_error();
  }
  return 0;
}
