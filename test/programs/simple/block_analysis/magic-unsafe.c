// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error(){}

int magic(int i) {
   
   if (i == 0) return 1;
   else return 0;

}


int main() {

   int count = 0;

   for (int i = 0; i < 3; i++) {
   
     int m = 0;
     if (i >= 0 && i < 2) m = magic(0);
     else m = magic(1);
     if (m != 1) count++; 
   
   }
   
   if (count == 1) reach_error();

}
