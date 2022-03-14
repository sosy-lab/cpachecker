// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include<assert.h>
void method_with_assert() {
    int c = 0;
    assert(c > 0);
}

int main() {
    method_with_assert();
}
