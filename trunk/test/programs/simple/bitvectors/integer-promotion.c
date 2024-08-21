// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  const unsigned short mask = 0x07FF;
  unsigned short i = 0;
  i = (i | ~mask);
  if (i > 0) {
    return 0;
  } else {
ERROR:
    return 1;
  }
}
