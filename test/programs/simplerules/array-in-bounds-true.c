// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Every array access of this program is inside the array: the type of the index allows only
// values that are in bounds. The program uses no pointer, so it cannot reach any other object.
extern unsigned char __VERIFIER_nondet_uchar(void);

int array[256];

int main(void) {
  unsigned char index = __VERIFIER_nondet_uchar();
  array[index] = 42;
  array[3] = array[index] + 1;
  return array[255];
}
