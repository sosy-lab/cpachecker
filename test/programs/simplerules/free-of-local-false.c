// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every execution frees an object that was never allocated.
extern void free(void *);

int main(void) {
  int x = 0;
  free(&x);
  return x;
}
