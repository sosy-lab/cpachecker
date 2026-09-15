// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  signed long int x = 0;

  if (-(1u > x ? 1u : x) <= 0) {
ERROR:
    return 1;
  }
  return 0;
}
