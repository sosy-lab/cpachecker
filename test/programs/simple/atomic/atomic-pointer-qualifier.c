// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Parsing of _Atomic to the right of a "*" (issue #1670): it qualifies the
// pointer itself, not the pointed-to object.

extern void reach_error(void);

typedef int *_Atomic atomic_int_ptr;

int g = 0;
int *plain_pointer = &g;

// _Atomic int: the object is atomic
_Atomic int atomic_int = 0;
// pointer to _Atomic int: the pointed-to object is atomic
_Atomic int *pointer_to_atomic = &atomic_int;

// _Atomic pointer to int: the pointer itself is atomic
int *_Atomic atomic_pointer = &g;
// the same, written with whitespace between "*" and "_Atomic"
int * _Atomic atomic_pointer2 = &g;
// _Atomic pointer to pointer to int
int **_Atomic atomic_pointer_to_pointer = &plain_pointer;
// pointer to _Atomic pointer to int
int *_Atomic *pointer_to_atomic_pointer = &atomic_pointer;
// _Atomic together with the other qualifiers
int *const _Atomic const_atomic_pointer = &g;
int *volatile _Atomic volatile_atomic_pointer = &g;
// several declarators, each with its own _Atomic
int *_Atomic first = &g, *_Atomic second = &g;
// array of _Atomic pointers
int *_Atomic atomic_pointers[2] = {&g, &g};
// via a typedef
atomic_int_ptr typedefed_atomic_pointer = &g;

struct s {
  int *_Atomic member;
  int *const _Atomic const_member;
};

// _Atomic pointers as function parameters
void takes_atomic_pointers(int *_Atomic p, int **_Atomic pp);

int main() {
  int x = 0;
  int *_Atomic local = &x;
  atomic_int_ptr typedefed_local = &x;
  struct s instance = {&x, &x};

  *local = 1;
  if (x != 1) {
    reach_error();
  }

  *typedefed_local = 2;
  if (*instance.member != 2) {
    reach_error();
  }

  *instance.const_member = 3;
  if (x != 3) {
    reach_error();
  }

  *atomic_pointer = 4;
  if (g != 4) {
    reach_error();
  }

  return 0;
}
