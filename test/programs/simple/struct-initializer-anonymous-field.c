// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct S1 {
    signed : 0;
    signed f : 1;
};

int main (void)
{
    struct S1 s = {-1};
    if (s.f)
        ERROR: goto ERROR;
    return 0;
}
