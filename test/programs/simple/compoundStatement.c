// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __assert_fail(const char *, const char *, unsigned int, const char *);

int f(int i) {
  return i+1;
}

int main() {
    int i = 0;
    ({ i++; int y = (i ? (i = i + 2) : 0); });
    int j = ({ int y; y = 4; ({ f(y + i++); }); });
    ({ if((i += j) != 12) { ERROR: __assert_fail("...", "test.c", 5, "..."); } });
    return i;
}
