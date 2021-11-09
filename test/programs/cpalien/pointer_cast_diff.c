// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void free (void *__ptr);

void crash() {
    int *ptr;
    int x = *ptr;
    free(ptr);
}

int length(char const *str) {
  char const *s = str;
  while ((int)*s != 0) { s++; }
  int result = (unsigned long)((long)s - (long)str);
  return result;
}

int length2(char const *str) {
  char const *s = str;
  while ((int)*s != 0) { s++; }
  int result = (unsigned long)(s - str);
  return result;
}

void main(void) {
  char source[] = {'A', '\000'};

  int len = length((char const *)(& source));
  if (len != 1) {
    // printf("len=%d\n", len);
    crash();
  }

  int len2 = length2((char const *)(& source));
  if (len2 != 1) {
    // printf("len2=%d\n", len2);
    crash();
  }

  char source2[] = {'A', 'A', 'A', '\000'};

  int len3 = length((char const *)(& source2));
  if (len3 != 3) {
    // printf("len3=%d\n", len3);
    crash();
  }

  int len4 = length2((char const *)(& source2));
  if (len4 != 3) {
    // printf("len3=%d\n", len4);
    crash();
  }

  char * source3 = "AAA";

  int len5 = length((char const *)(& source3));
  if (len5 < 3) {
    // printf("len5=%d\n", len5);
    crash();
  }

  int len6 = length2((char const *)(& source3));
  if (len6 < 3) {
    // printf("len6=%d\n", len6);
    crash();
  }
}
