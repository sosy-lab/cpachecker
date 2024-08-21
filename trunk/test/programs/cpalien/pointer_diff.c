// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void *malloc (long int __size);
extern void free (void *__ptr);

typedef struct example_t {
  int e1;
  int e2;
  int e3;
} example_t;

void crash() {
    int *ptr;
    int x = *ptr;
    free(ptr);
}

void testLong() {
  long long int al[10] = {0,0,0,0,0,0,0,0,0,0};
  long long int *pl = &(al[3]);
  long long int *ql = &(al[7]);
  if (ql - pl != 4) {
    crash();
  }
  if (&(al[6]) - &(al[1]) != 5) {
    crash();
  }
}

void testInt() {
  int ai[10] = {0,0,0,0,0,0,0,0,0,0};
  int *pi = &(ai[3]);
  int *qi = &(ai[7]);
  if (qi - pi != 4) {
    crash();
  }
  if (&(ai[6]) - &(ai[1]) != 5) {
    crash();
  }
}

void testStruct() {
  example_t* ex = malloc(sizeof(example_t));

  ex->e1 = 3;
  ex->e1 = 4;
  ex->e1 = 5;

  int* f1 = &ex->e2;
  int* f2 = (int*)ex;
  int diff = (char*)(&(ex)->e2) - (char*)(ex);
  if (diff != 4) {
    crash();
  }
  if (f1 - f2 != 1) {
    crash();
  }
  if ((char*)(&ex->e2) - (char*)(ex) != 4) {
    crash();
  }
  if (&ex->e2 - (int*)ex != 1) {
    crash();
  }

  free(ex);
}

int main() {
  testInt();
  testLong();
  testStruct();
}
