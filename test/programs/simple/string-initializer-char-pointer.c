// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// A string literal initializes a pointer to (non-const) char.
typedef char *string;
char *global = "abc";
static char *staticGlobal = "-";
string typedefGlobal = "xy";
char *firstElement = &("abc"[0]);

int main(void) {
  char *local = "";
  char *a = global;
  char **p = &staticGlobal;
  string b = typedefGlobal;
  char *f = firstElement;
  char *localElement = &("xy"[1]);
  if (a != global || *p != staticGlobal || b != typedefGlobal || local != local
      || f != firstElement || localElement != localElement) {
  ERROR:
    return 1;
  }
  return 0;
}
