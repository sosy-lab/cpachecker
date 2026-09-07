// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* C11 makes these implementation-defined; Linux/GCC/Clang use arithmetic right shift. */
  if ((-3 >> 1) != -1) {
    goto ERROR;
  }
  if ((-5 >> 2) != -2) {
    goto ERROR;
  }
  if (((-2147483647 - 1) >> 16) != -32768) {
    goto ERROR;
  }
  if (((-2147483647 - 1) >> 31) != -1) {
    goto ERROR;
  }
  if ((-3L >> 1) != -2L) {
    goto ERROR;
  }
  if (((-9223372036854775807L - 1L) >> 32) != -2147483648L) {
    goto ERROR;
  }
  if (((-9223372036854775807L - 1L) >> 63) != -1L) {
    goto ERROR;
  }
  if ((-3LL >> 1) != -2LL) {
    goto ERROR;
  }
  if (((-9223372036854775807LL - 1LL) >> 32) != -2147483648LL) {
    goto ERROR;
  }
  if (((-9223372036854775807LL - 1LL) >> 63) != -1LL) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
