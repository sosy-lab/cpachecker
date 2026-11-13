// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
extern void abort(void);
void reach_error(void) { abort(); }
extern int strcmp(const char *s1, const char *s2);

// Machine model: LP64. Expected: false for unreach-call.
int main(void) {
  char a[] = "ab";

  // Reachable: "ab" is smaller than "ac".
  if (strcmp(a, "ac") < 0) {
    reach_error();
  }
  return 0;
}
