// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <math.h>
#include <stdlib.h>

void reach_error() {
  abort();
}

int main() {
  int exp;
  if (frexp(0, &exp)  == 13.0) {
    reach_error();
  }
  return 0;
}