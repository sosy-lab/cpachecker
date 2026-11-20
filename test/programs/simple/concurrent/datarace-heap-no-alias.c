// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The threads access different objects on the heap, also if malloc may return null.

typedef unsigned long int pthread_t;
union pthread_attr_t { char __size[56]; long int __align; };
typedef union pthread_attr_t pthread_attr_t;
extern int pthread_create(pthread_t *__restrict __newthread,
    const pthread_attr_t *__restrict __attr,
    void *(*__start_routine)(void *), void *__restrict __arg);

extern void *malloc(unsigned int size);

struct node {
  int v;
  struct node *next;
};

struct node *head, *other;

void *writer(void *arg) {
  struct node *n = head;
  n->v = 1;
  return 0;
}

int main() {
  head = malloc(sizeof(struct node));
  other = malloc(sizeof(struct node));
  pthread_t t;
  pthread_create(&t, 0, writer, 0);
  int r = other->v;
  return 0;
}
