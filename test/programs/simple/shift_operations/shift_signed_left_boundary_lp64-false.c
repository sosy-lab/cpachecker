// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* Every signed left shift below has a nonnegative operand and a representable result. */
  if ((1073741823 << 1) != 2147483645) {
    goto ERROR;
  }
  if ((536870911 << 2) != 2147483644) {
    goto ERROR;
  }
  if ((32767 << 16) != 2147418112) {
    goto ERROR;
  }
  if ((1 << 30) != 1073741824) {
    goto ERROR;
  }
  if ((4611686018427387903L << 1) != 9223372036854775806L) {
    goto ERROR;
  }
  if ((2305843009213693951L << 2) != 9223372036854775804L) {
    goto ERROR;
  }
  if ((4611686018427387903LL << 1) != 9223372036854775806LL) {
    goto ERROR;
  }
  if ((2305843009213693951LL << 2) != 9223372036854775804LL) {
    goto ERROR;
  }
  if ((1LL << 62) != 4611686018427387904LL) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
