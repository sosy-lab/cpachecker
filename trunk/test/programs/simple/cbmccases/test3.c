// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main()
{
int __BLAST_NONDET ;
int s;
int a;
int b;

 s = __BLAST_NONDET;
 a = __BLAST_NONDET;
 b = __BLAST_NONDET;

  if (s == a) {
    if (b == 1) {
      b = 0;
    } else {
      goto _L;
    }
  } else {
    _L: ;
    a--;
  }
      ERROR:
      return (-1073741823);
}

