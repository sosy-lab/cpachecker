// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int x=0;

int main() {
    int a, b;
    
    if (!(a&&b)) {
        x=1;
    }

    if (!a||!b) {
        x=2;
    }
    
    // 5x NOT is 1x NOT
    if (!(!(!(!(!(a&&b)))))) {
        x=3;
    }
    
    // 4x NOT is no NOT
    if (!(!(!(!(a&&b))))) {
        x=4;
    }
    
    return 0; 
}
