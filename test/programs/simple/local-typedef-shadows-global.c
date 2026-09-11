// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void reach_error() {}

// Global typedef: one field, sizeof(T) == sizeof(int)
typedef struct GlobalT {
  int a;
} T;

int main() {
  // Local typedef of the same name, declared inside a function body: it
  // must shadow the global "T" for uses inside this scope, not be silently
  // ignored in favor of the global one. Uses an explicit, distinct struct
  // tag ("LocalT") so this only exercises typedef-name shadowing, not the
  // separate (and separately unsupported) case of two anonymous structs
  // that would end up sharing the same synthesized tag name.
  typedef struct LocalT {
    int a;
    int b;
  } T;

  T v;
  v.a = 1;
  v.b = 2;
  if (sizeof(v) != 2 * sizeof(int)) {
    goto ERROR;
  }
  return 0;
ERROR:
  reach_error();
  return -1;
}
