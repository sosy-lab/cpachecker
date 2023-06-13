// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned int size_t;
void * __attribute__((__cdecl__)) malloc (size_t __size) ;
int main() {
    char *s = malloc(3 * sizeof(char));
    char *p = s;
    s[2] = 0;
    while (*p != 0) {
      p++;
    }
    if (p - s > 2) {
      ERROR:
      return 1;
    }
}
