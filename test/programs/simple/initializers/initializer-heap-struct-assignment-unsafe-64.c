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

extern void *malloc(unsigned long size);
extern void free(void *ptr);

struct Values {
  char c_first;
  char c_second;
  short s_first;
  short s_second;
  int i_first;
  int i_second;
  long l_first;
  long l_second;
  long long ll_first;
  long long ll_second;
};


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Whole-structure assignment copies zeros from every omitted compound-literal member.
int main(void) {

  int all_expected_checks_fail = 0;

  struct Values *p = malloc(sizeof *p);
  if (p == 0) {
    return 0;
  }

  *p = (struct Values){11, 22, 11, 22, 11, 22, 11, 22, 11, 22};

  *p = (struct Values){.c_first = 33, .s_first = 33, .i_first = 33, .l_first = 33, .ll_first = 33};

  all_expected_checks_fail = all_expected_checks_fail || (p->c_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->c_second != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->s_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->s_second != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->i_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->i_second != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->l_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->l_second != 0);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll_second != 0);

  free(p);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
