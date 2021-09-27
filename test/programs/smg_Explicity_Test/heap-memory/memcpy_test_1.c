// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <string.h>
#include <stdlib.h>
struct test {
  int t_i;
  long t_l;
  char t_c;
  long t_l1;
};
char a[1024][512];

void read(int line, int row, int size, void *buf)
{
   memcpy(buf, &a[line][row], size);
}

void write(int line, int row, int size, void *buf)
{
   memcpy(&a[line][row], buf, size);
}

int main(void)
{
   struct test t = {.t_c = 't', .t_l = 1}, s = {.t_c = 's', .t_l = 1};
   char *buf = malloc(1024);
   write(2, 0, sizeof(struct test), &t);
   write(10, 0, sizeof(struct test), &s);
   if (!buf) {
     return 1;
   }
   read(2, 0, 512, &buf[0]);
   read(10, 0, 512, &buf[512]);
   struct test *first = (struct test *)&buf[0];
   struct test *second = (struct test *)&buf[512];
   if (first->t_l != second->t_l) {
     return 2;
   }
   if (first->t_c == second->t_c) {
     return 2;
   }
   free(buf);
   return 0;
}

