// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include "EqualNamedTypes.h"

struct sameNamed {
int x;
};


void setFirst(struct Pair *p, int val) {
  struct sameNamed str = {.x = 5};
  p->a = str.x;
}
