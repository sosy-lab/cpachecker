// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// break inside while loop condition
int main() {
    while (1) {
        int a = 5;
        while(({break; a++;})) { }
        
        if(a == 5) {
            ERROR: // reachable
                return 0;
        }
    }
    return 1;
}
