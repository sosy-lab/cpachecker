// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int a;
    if (a!=5) 
    if (a!=6)
    if ((a!=7)==7) { // never fullfilled, because 7 is not a boolean value
        ERROR:goto ERROR;
    }
    
    return (0);
}
