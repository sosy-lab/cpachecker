// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);

void __VERIFIER_assert(int cond) {
  if (!cond) {
  ERROR: abort();
  }
}

int len(char* str) {
  char* pos = str;
  while (*pos != 0) {
    pos++;
  }
  return pos - str;
}

int main() {
  // Use an array as pointer for a function call
  char msg[] = "Error";
  __VERIFIER_assert(len(msg) == 5);
}
