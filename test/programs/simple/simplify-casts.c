// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int f(unsigned int cmd) {
  switch (cmd) {
  case (1UL | (unsigned long )4 ):
    ERROR: goto ERROR;
  break;
}
}

int main() {

  f(1UL | (unsigned long )3);
}
