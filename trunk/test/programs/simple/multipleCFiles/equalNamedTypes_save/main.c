// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include "EqualNamedTypes.h"

struct sameNamed {
int a;
int b;
};

__VERIFIER_assert(int cond) {
  if (cond == 0) {
    ERROR: goto ERROR;
  }
}

int main(void) {
  struct Pair *p;
  setFirst(p, 25);

  __VERIFIER_assert(p->a == 25);

  struct sameNamed s = {.a=10, .b=100};
  s.b=10;

  __VERIFIER_assert(s.a == s.b);

  return p->a;
}
