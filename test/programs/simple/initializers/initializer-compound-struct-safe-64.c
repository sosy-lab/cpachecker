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
  char second;
};

struct s_record {
  short first;
  short second;
};

struct i_record {
  int first;
  int second;
};

struct l_record {
  long first;
  long second;
};

struct ll_record {
  long long first;
  long long second;
};


// Expected: true (safe for unreach-label). Machine model: LP64 (64).
// Each compound literal supplies first member 11 and implicit second member zero.
// All asserted observations match the expected behavior, so ERROR is unreachable.
int main(void) {

  struct c_record c = (struct c_record){11};
  struct s_record s = (struct s_record){11};
  struct i_record i = (struct i_record){11};
  struct l_record l = (struct l_record){11};
  struct ll_record ll = (struct ll_record){11};

  __VERIFIER_assert(c.first == 11);
  __VERIFIER_assert(c.second == 0);
  __VERIFIER_assert(s.first == 11);
  __VERIFIER_assert(s.second == 0);
  __VERIFIER_assert(i.first == 11);
  __VERIFIER_assert(i.second == 0);
  __VERIFIER_assert(l.first == 11);
  __VERIFIER_assert(l.second == 0);
  __VERIFIER_assert(ll.first == 11);
  __VERIFIER_assert(ll.second == 0);

  return 0;
}
