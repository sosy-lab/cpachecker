// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

enum Weekday {MON=1, TUE, WED, THU, FRI, SAT, SUN=0};

int main(void) {
    enum Weekday x = MON;
    while (x <= SAT) {
        x = x + 1;
    }
    if(x % 7 != SUN) {
        ERROR: return 1;
    }
    return 0;
}
