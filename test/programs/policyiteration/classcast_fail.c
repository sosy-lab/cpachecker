// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

extern int __undef();
extern int __undef2();

int main() {
    int x = 0;
    while (__undef()) {
        x = 1;
        while (__undef2()) {
            x = x + 3;
            if (x == 4) {break;}
        }
    }
    assert(x >= 0 && x <= 4);
}
