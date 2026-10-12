// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This is a heavily modified and simplified version of
// product-lines/minepump_spec4_product45.cil.c in SV-Benchmarks.

int water = 1;     // 0..2
int methane = 0;   // 0/1
int pump = 0;      // 0/1

extern int __VERIFIER_nondet_int();

int t() {return __VERIFIER_nondet_int();}

int main(void) {
    for (int i = 0; i < 3; i++) {
        /* environment */

        if (pump && water > 0) water--;
        if (t() && water < 2) water++;
        if (t()) methane ^= 1;

        /* controller */
        if (pump && methane) pump = 0;
        if (!pump && water == 2 && !methane) pump = 1;


        /* safety */
        if (pump && water == 0) reach_error();
        
    
    }
}
