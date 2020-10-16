// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int counter = 0;
extern void* unknown_func();

void assume_equality(void* v) {
    if (v != 0) {
        counter++;
    }
}

void* f() {
    void *res;
    res = unknown_func();
    
    assume_equality(res);
    
    return res;
}

int main() {
    void* tmp;
    
    tmp = f();
    
    if (tmp == 0) {
        return 0;
    }
    
    if (counter == 0) {
        ERROR: goto ERROR;
    }
}
