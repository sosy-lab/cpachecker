// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef long intptr_t;
struct my_struct {
   struct my_struct *p ;
   int a ;
   int b ;
};

int main() {
    struct my_struct *tl ;

    if (((unsigned long )((intptr_t )tl) & (__alignof__(tl) - 1UL)) == 0UL) {
ERROR:
      return 1;
    } else {
      return 0;
    }
}
