// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int i = 6;
  for (i = 0; i < 6; i++) {
  }
  if (i != 6) {
  ERROR:
    return 1;
  }
  return 0;
}
