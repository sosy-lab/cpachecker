// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// It is an accelerated version of the sv-benchmarks task easy2-2.c,
// which is under BSD 2-Clause "Simplified" license, curtesy of
// University of Freiburg.
// The changes are licensed under Apache-2.0 license.
//
// SPDX-FileCopyrightText: 2013 University of Freiburg
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
// SPDX-License-Identifier: BSD-2-Clause

typedef enum {false, true} bool;

extern int __VERIFIER_nondet_int(void);

int main() {
    int x, y, z;
        x = 0;
    y = 0;
    z = __VERIFIER_nondet_int();
    if (z > 0) {
        if (z > 2) {
            if (z > 0) {
                x = x + 1;
                y = y - 1;
                z = z - 1;
		x = x + (z - 1);
            	y = y + (1 - z);
            	z = z + (1 - z);
            	if (z > 0) {
                	x = x + 1;
                	y = y - 1;
                	z = z - 1;
            	}
            }
        }
    } else {
    	if (z > 0) {
            x = x + 1;
            y = y - 1;
            z = z - 1;
        }
        if (z > 0) {
            x = x + 1;
            y = y - 1;
            z = z - 1;
        }

    }
}
