// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

extern void *malloc(unsigned long size);
extern void free(void *ptr);
extern int snprintf(char *dest, unsigned long size, const char *format, ...);


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// Ten characters and a terminator fill the eleven-byte destination.
int main(void) {

  int all_expected_checks_fail = 0;

  char *p = malloc(11UL);
  if (p == 0) {
    return 0;
  }

  for (int i = 0; i < 11; ++i) {
    p[i] = 'Z';
  }

  snprintf(p, 11UL, "%s", "abcdefghijklm");

  all_expected_checks_fail = all_expected_checks_fail || (p[0] != 'a');
  all_expected_checks_fail = all_expected_checks_fail || (p[1] != 'b');
  all_expected_checks_fail = all_expected_checks_fail || (p[2] != 'c');
  all_expected_checks_fail = all_expected_checks_fail || (p[3] != 'd');
  all_expected_checks_fail = all_expected_checks_fail || (p[4] != 'e');
  all_expected_checks_fail = all_expected_checks_fail || (p[5] != 'f');
  all_expected_checks_fail = all_expected_checks_fail || (p[6] != 'g');
  all_expected_checks_fail = all_expected_checks_fail || (p[7] != 'h');
  all_expected_checks_fail = all_expected_checks_fail || (p[8] != 'i');
  all_expected_checks_fail = all_expected_checks_fail || (p[9] != 'j');
  all_expected_checks_fail = all_expected_checks_fail || (p[10] != '\0');

  free(p);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
