// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void reach_error(); 


struct st {
    int id; 
};


int main() 
{
    struct st s; 
    s.id = 1;
    for (int i = 1; i <= 10; i++) {
        s.id *= 2;
    }

    if (s.id != 1024) reach_error();
}