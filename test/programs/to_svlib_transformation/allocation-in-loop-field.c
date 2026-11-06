// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort();
void reach_error(){}
extern void *malloc(unsigned long);
extern int __VERIFIER_nondet_int();
struct node { int data; struct node *next; };
// Every iteration of the loop allocates a new block and stores a nondeterministic value in it, so
// the value that is read after the loop can be any one and the error is reachable. The assignment
// of the result of the allocation has to be an assignment and not an assumption, because an
// assumption about the pointer would contradict its value from the previous iteration.
int main() {
  struct node *head = 0;
  int len = 2;
  while (len > 0) {
    struct node *n = malloc(sizeof(struct node));
    if (!n) { return 0; }
    n->data = __VERIFIER_nondet_int();
    n->next = head;
    head = n;
    len--;
  }
  if (head->data != 0) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
