// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
    int k = 5;
    
    for (k = 10; k < 100; k++) {
        
        if (k == 0) {
            break;
        }
        if (k == 1) {
            break;
        } else if (k == 2) {
            break;
        } else if (k == 3) {
            break;
        }
    }
    
    return (0);
}
