// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort();
void reach_error(){}

// A parameter and a local variable may have the name of a global variable, but the generated
// program has no scopes, so such a variable is renamed there and the global one keeps its name.
int g = 1;
int count = 9;

int readG() {
  return g;
}

void checkParameter(int g) {
  g = g + 1;
  if (g != 8 || readG() != 1) {
    ERROR: {reach_error();abort();}
  }
}

void checkLocal() {
  int g = 5;
  static int count = 0;
  count = count + 1;
  if (g != 5 || count != 1 || readG() != 1) {
    ERROR: {reach_error();abort();}
  }
}

int main() {
  checkParameter(7);
  checkLocal();
  if (g != 1 || count != 9) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
