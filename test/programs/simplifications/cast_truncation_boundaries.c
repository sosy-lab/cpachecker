// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  if ((unsigned char)256 != 0) goto ERROR;
  if ((unsigned char)257 != 1) goto ERROR;
  if ((signed char)255 != -1) goto ERROR;
  if ((unsigned short)65536 != 0) goto ERROR;
  if ((unsigned short)65537 != 1) goto ERROR;

  return 0;
ERROR:
  return 1;
}
