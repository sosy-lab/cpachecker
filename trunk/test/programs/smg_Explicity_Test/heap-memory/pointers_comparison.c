// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned char __uint8_t;
typedef __uint8_t uint8_t;
struct const_passdb
{
  char const
  *filename;
  char def[9U];
  uint8_t off[9U];
  uint8_t numfields;
  uint8_t size_of;
};

int main() {
    struct const_passdb *test;
    test = malloc(sizeof(struct const_passdb));
    if (test) {
        if ( test->def != test->off ) {
            free(test);
        }
    }
    return 0;
}
