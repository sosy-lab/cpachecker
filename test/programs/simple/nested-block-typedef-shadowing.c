// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

// A global typedef of the same name as the local ones below. Its purpose
// is to force a real test of the local typedef stack's per-block lookup
// order: without correctly-scoped local typedef resolution, a local use of
// "T" could wrongly fall back to this global entry instead of the nearest
// enclosing local one.
typedef struct GlobalT {
  int a;
} T;

int main() {
  // Outer local typedef shadows the global one: two fields.
  typedef struct OuterT {
    int a;
    int b;
  } T;

  {
    // Inner-block typedef shadows the outer local one: three fields. Uses
    // of "T" inside this block must resolve to this one.
    typedef struct InnerT {
      int a;
      int b;
      int c;
    } T;

    T inner;
    inner.a = 1;
    inner.b = 2;
    inner.c = 3;
    if (sizeof(inner) != 3 * sizeof(int)) {
      goto ERROR;
    }
  }

  // Once the inner block has ended, "T" must resolve back to the outer
  // local typedef again (not the global one, and not the now-out-of-scope
  // inner one).
  T outer;
  outer.a = 4;
  outer.b = 5;
  if (sizeof(outer) != 2 * sizeof(int)) {
    goto ERROR;
  }

  return 0;
ERROR:
  reach_error();
  return -1;
}
