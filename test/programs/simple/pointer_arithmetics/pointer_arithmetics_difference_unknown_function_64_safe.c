// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// strrchr is supposed to be unknown (i.e. not modeled by the analysis), so its result is not known as a pointer to the analysis.
extern char *strrchr(const char *s, int c);

// Machine model: LP64. Expected: true for valid-memsafety.
// The difference of the two pointers is computed, but never used to access memory.
int main(void) {
  char path[4] = "a/b";
  const char *name = path;
  const char *slash = strrchr(name, '/');
  const char *base;
  if (slash != 0) {
    base = slash + 1;
  } else {
    base = name;
  }

  if (base - name >= 1) {
    return 1;
  }
  return 0;
}
