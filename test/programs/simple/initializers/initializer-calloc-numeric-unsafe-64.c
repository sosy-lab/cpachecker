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


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// calloc's all-zero bytes represent zero for every tested integer element; padding is not inspected.
int main(void) {

  int all_expected_checks_fail = 0;

  struct Numbers *p = calloc(1UL, sizeof *p);
  if (p == 0) {
    return 0;
  }

  all_expected_checks_fail = all_expected_checks_fail || (p->c[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->c[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->c[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->s[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->s[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->s[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->i[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->i[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->i[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->l[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->l[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->l[2] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll[0] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll[1] != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll[2] != 0);

  free(p);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
