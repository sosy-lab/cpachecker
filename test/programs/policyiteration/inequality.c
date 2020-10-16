// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }
extern _Bool __VERIFIER_nondet_bool();

int main() {
   int i = 0;
   while(__VERIFIER_nondet_bool()){
      if (i == 4) {
         i = 0;
      }
      i++;
   }
   assert(i >= 0 && i < 5);
}
