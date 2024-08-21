// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

const int BOUND = 10;

int test(int input);

int main() {
    int a = test(0);
    int b = test(0);
    assert(a + b == BOUND);
}

int test(int input) {
    for (int i=0; i<BOUND; i++) {
        input++;
    }
    return input;
}

