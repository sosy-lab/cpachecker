// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The gotos form a loop with the heads B and C, which the paths visit in turn.
// The heads share one loop counter, so with k = 1 every abstraction of an
// induction step that starts at B reaches B again. An induction step must also
// start at C, otherwise no step checks the assertion after B.

extern void abort(void);
void reach_error() { abort(); }

int main() {
  int x = 0;
  int p = 0;
A:
  if (p == 2) {
    goto B;
  }
  goto C;
B:
  if (x == 1) {
    reach_error();
  }
  p = 0;
  if (p == 3) {
    goto A;
  }
  goto C;
C:
  x = 1;
  p = 0;
  if (p == 4) {
    goto A;
  }
  goto B;
}
