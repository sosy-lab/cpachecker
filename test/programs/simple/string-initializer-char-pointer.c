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

int main(void) {
  char *local = "";
  char *a = global;
  char **p = &staticGlobal;
  string b = typedefGlobal;
  if (a != global || *p != staticGlobal || b != typedefGlobal || local != local) {
  ERROR:
    return 1;
  }
  return 0;
}
