// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void reach_error() { ERROR: assert(0); }

void f(int i) {
  if(i == 0) {
  reach_error();
  }
}

int main() {
  f(0);
}
