// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every execution terminates, also those in which an allocation fails.
typedef unsigned int size_t;
extern void *malloc(size_t size);
extern void free(void *ptr);
extern void exit(int status);

struct node {
  int value;
  struct node *next;
};

int main(void) {
  struct node *list = 0;
  for (int i = 0; i < 10; i++) {
    struct node *n = malloc(sizeof(struct node));
    if (n == 0) {
      exit(1);
    }
    n->value = i;
    n->next = list;
    list = n;
  }
  int sum = 0;
  while (list != 0) {
    struct node *next = list->next;
    sum += list->value;
    free(list);
    list = next;
  }
  return sum == 45 ? 0 : 1;
}
