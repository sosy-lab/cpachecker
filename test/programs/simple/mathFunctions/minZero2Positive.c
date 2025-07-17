// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <math.h>
#include <stdlib.h>

void reach_error() {
  abort();
}

int main() {
  // fmin(+0,-0) = +0
  if (!signbit(fmin(0.0, -0.0))) {
    reach_error();
  }
}
