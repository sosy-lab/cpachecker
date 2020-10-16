// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void test(int x) {
 if (!x) {
ERROR: goto ERROR;
 }
}

/*
 * We consider the following program as unsafe, because the possibility of the
 * error occuring can not be ruled out.
 * It is not possible to give a general range for which the problem can not occur.
 * Valid ranges could be set with a memory model.
 */
void main() {
  int a = 128;
  int* p = &a;
  test(p != *p);
}
