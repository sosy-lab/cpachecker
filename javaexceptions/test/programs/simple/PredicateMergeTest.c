// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int counter = 0;
extern void* unknown_func();

void* f() {
    void *res;
    res = unknown_func();
    
    return res;
}

int main() {
    int tmp = 0;
    
    f();
    
    if (counter != 0) {
        ERROR: goto ERROR;
    }
    
    if (tmp != 0) {
        goto ERROR;
    }
}
