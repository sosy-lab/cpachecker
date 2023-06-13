// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc (long int __size);
extern void free (void *__ptr);

void crash() {
    int *ptr;
    int x = *ptr;
    free(ptr);
}

int main() {
  int ai[1] = {0x12345678};
  char* chr = (char*)ai;

  if (ai[0] != 0x12345678) {
    crash();
  }

  if (chr[0] != 0x78) {
    crash();
  }
  if (chr[1] != 0x56) {
    crash();
  }
  if (chr[3] != 0x12) {
    crash();
  }

  char chr1 = chr[1];
  if (chr1 != 0x56) {
    crash();
  }

  char *chrp1 = &chr[1];
  if (*chrp1 != 0x56) {
    crash();
  }
}
