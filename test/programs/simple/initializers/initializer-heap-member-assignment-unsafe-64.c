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
// Each second member was explicitly seeded to twenty-two and is unchanged by a first-member assignment.
int main(void) {

  int all_expected_checks_fail = 0;

  struct Values *p = malloc(sizeof *p);
  if (p == 0) {
    return 0;
  }

  *p = (struct Values){11, 22, 11, 22, 11, 22, 11, 22, 11, 22};

  p->c_first = 33;
  p->s_first = 33;
  p->i_first = 33;
  p->l_first = 33;
  p->ll_first = 33;

  all_expected_checks_fail = all_expected_checks_fail || (p->c_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->c_second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (p->s_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->s_second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (p->i_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->i_second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (p->l_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->l_second != 22);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll_first != 33);
  all_expected_checks_fail = all_expected_checks_fail || (p->ll_second != 22);

  free(p);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
