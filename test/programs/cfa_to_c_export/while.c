// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int x = 10;
    int y;
    int i = 0;
    while (i < x) {
        y = y + i;
        i = i + 1;
    }
    return y;
}
