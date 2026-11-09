// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Pointers initialized with string literals point to the literals,
// the characters must not be written through the (still unset) pointers.

extern void abort(void);
void reach_error(void) { abort(); }

struct proto {
  const char *name;
  int type;
};

static const struct proto protos[] = {{"ab", 1}, {"cd", 2}};

int main(void) {
  const char *p = "xy";
  if (protos[0].name[0] == 'a' && protos[1].name[0] == 'c' && p[1] == 'y') {
    reach_error();
  }
  return 0;
}
