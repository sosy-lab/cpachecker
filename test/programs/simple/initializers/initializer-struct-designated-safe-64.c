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

struct c_record {
  char first;
  char middle;
  char last;
};

struct s_record {
  short first;
  short middle;
  short last;
};

struct i_record {
  int first;
  int middle;
  int last;
};

struct l_record {
  long first;
  long middle;
  long last;
};

struct ll_record {
  long long first;
  long long middle;
  long long last;
};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// The designated middle member is 22; both omitted surrounding members are zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct c_record c = {.middle = 22};
  struct s_record s = {.middle = 22};
  struct i_record i = {.middle = 22};
  struct l_record l = {.middle = 22};
  struct ll_record ll = {.middle = 22};

  __VERIFIER_assert(c.first == 0);
  __VERIFIER_assert(c.middle == 22);
  __VERIFIER_assert(c.last == 0);
  __VERIFIER_assert(s.first == 0);
  __VERIFIER_assert(s.middle == 22);
  __VERIFIER_assert(s.last == 0);
  __VERIFIER_assert(i.first == 0);
  __VERIFIER_assert(i.middle == 22);
  __VERIFIER_assert(i.last == 0);
  __VERIFIER_assert(l.first == 0);
  __VERIFIER_assert(l.middle == 22);
  __VERIFIER_assert(l.last == 0);
  __VERIFIER_assert(ll.first == 0);
  __VERIFIER_assert(ll.middle == 22);
  __VERIFIER_assert(ll.last == 0);

  return 0;
}
