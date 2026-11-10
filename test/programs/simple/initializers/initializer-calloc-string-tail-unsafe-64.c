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

extern void *calloc(unsigned long count, unsigned long size);
extern void free(void *ptr);
extern void *memcpy(void *dest, const void *src, unsigned long count);


// Expected: false (unsafe for unreach-label). Machine model: LP64 (64).
// calloc initializes all bytes to zero; the payload-only copy preserves the six zero tail bytes.
int main(void) {

  int all_expected_checks_fail = 0;

  char *p = calloc(19UL, 1UL);
  if (p == 0) {
    return 0;
  }

  memcpy(p, "abcdefghijklm", 13UL);

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
  all_expected_checks_fail = all_expected_checks_fail || (p[10] != 'k');
  all_expected_checks_fail = all_expected_checks_fail || (p[11] != 'l');
  all_expected_checks_fail = all_expected_checks_fail || (p[12] != 'm');
  all_expected_checks_fail = all_expected_checks_fail || (p[13] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (p[14] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (p[15] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (p[16] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (p[17] != '\0');
  all_expected_checks_fail = all_expected_checks_fail || (p[18] != '\0');

  free(p);

  // Every deliberately negated check is false; only then does this one assertion reach ERROR.
  __VERIFIER_assert(all_expected_checks_fail);

  return 0;
}
