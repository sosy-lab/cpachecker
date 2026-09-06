// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort();
void reach_error(){}

// The names of these variables are reserved words of SV-LIB, so they have to be quoted in the
// generated program.
static int reset = 5;
static int choice;

int main() {
  int label = 3;
  reset = 0;
  choice = reset + label;
  if (choice == 1) {
    ERROR: {reach_error();abort();}
  }
  return 0;
}
