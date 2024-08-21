// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int a = 10 + 10;
    int b = 1+2+3+4+5;
    
    if (b-2 != 6+7) {
        goto ERROR;
    }
    
    if (10*2!=a) {
        goto ERROR;
    }
    
    return (0);
    ERROR: 
    return (-1);
}
