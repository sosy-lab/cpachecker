// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

extern int __VERIFIER_nondet_int(void);

 
struct sll {
  struct sll *next;
};

struct sll* create(void) {
  struct sll *sll = malloc(sizeof(struct sll));
  if (sll == 0) {
    return 0;
  }
  sll->next = 0;
  struct sll *now = sll;
  while(__VERIFIER_nondet_int()) {
    now->next = malloc(sizeof(struct sll));
    if (now->next == 0) {
      return sll;
    }
    now = now->next;
    now->next = 0;
  }
  return sll;
}

void destroy(struct sll* now) {
  while(now->next != 0) {
    struct sll *prev = now;
    now = now->next;
    free(prev);
  }
  free(now);
}

// Create a singly-linked list and destroy it again. MemorySafety is not violated. 
// This program can safely run with 32 and 64 bit machine models.
int main() {
  struct sll *sll = create();
  if (sll == 0) {
    return 1;
  }
  destroy(sll);
  return 0;
}
