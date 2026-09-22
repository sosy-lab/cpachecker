// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// sm() does not terminate, so the whole program does not terminate.
// The call to nr() makes the rest of main() unreachable, because nr() calls a
// function with the attribute 'noreturn'. get_one() is called only from that
// unreachable part, but the function one() that it calls is also called from
// the non-terminating loop in sm(), i.e., from reachable code.
// Removing the unreachable part of main() must not remove one() or get_one().

int one() {
    return 1;
}

int get_one() {
  return one();
}

extern void nonret(void) __attribute__((__noreturn__));

void nr() { nonret(); }

void sm(int n) {
  if (n > 0) {
    one();
    while (1) 
      ;
  }
}

int main(void) {
  sm(1);
  nr();
  int c2 = get_one();
  return 0;
}
