// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(); 


int main() { 
    int a[2] = {1, 2}; 
    int sum = 0;
    for (int i = 0; i < 2; i++) {
        sum += a[i]; 
    }
    
    if (sum != 3) { reach_error(); } 
}