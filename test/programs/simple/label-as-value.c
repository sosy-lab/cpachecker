// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  long unsigned int i;
start:
  i = (long unsigned int) && start;
  if ((i && i) != ((i) && i)) {
ERROR:
    return 1;
  }
  return 0;
}
