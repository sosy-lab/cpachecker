// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  int i = 0;
  int a = 0;

  while (1) {
    if (i == 10) {
      goto LOOPEND;
    }

    if (i == 20) {
       goto LOOPEND;
    } else {
       i++;
       a++;
    }

  }

  LOOPEND:
  __VERIFIER_assert(i == 20);	
  return (1);


}

