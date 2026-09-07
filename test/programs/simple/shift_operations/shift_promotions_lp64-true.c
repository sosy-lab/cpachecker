// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* Narrow integer operands are promoted to int before the shift. */
  const unsigned char uc = 255;
  const signed char sc = 127;
  const unsigned short us = 65535;
  const short ss = 32767;

  if ((uc << 8) != 65280) {
    goto ERROR;
  }
  if ((uc << 23) != 2139095040) {
    goto ERROR;
  }
  if ((uc >> 31) != 0) {
    goto ERROR;
  }
  if ((sc << 16) != 8323072) {
    goto ERROR;
  }
  if ((us << 15) != 2147450880) {
    goto ERROR;
  }
  if ((us >> 16) != 0) {
    goto ERROR;
  }
  if ((ss << 16) != 2147418112) {
    goto ERROR;
  }
  if ((ss >> 15) != 0) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
