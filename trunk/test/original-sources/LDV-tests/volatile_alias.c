// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Bug #260 - int is not aliased with volatile int
// RESULT: yes, it flows through.

int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

int main()
{
        int volatile a = 4;
        int * const p = &a;
        p = &a;
        a = a - 4;
        if (*p != 0){
                ERROR: goto ERROR;
        }
        return 0;
}

