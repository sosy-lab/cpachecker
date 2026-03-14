// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
  signed char c = -5;
  signed char d = (signed char)(int)c;
  unsigned char u = (unsigned char)(signed char)-1;

  if (d != -5) goto ERROR;
  if (u != 255) goto ERROR;

  return 0;
ERROR:
  return 1;
}
