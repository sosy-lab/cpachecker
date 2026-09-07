// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
  /* Destination conversion is separate from shift evaluation; all values remain representable. */
  const unsigned char uc = 127;
  int shifted_uc = uc << 1;
  unsigned char stored_uc = (unsigned char)shifted_uc;
  if (shifted_uc != 254) {
    goto ERROR;
  }
  if (stored_uc != 254) {
    goto ERROR;
  }

  const short ss = 16383;
  int shifted_ss = ss << 1;
  short stored_ss = (short)shifted_ss;
  if (shifted_ss != 32766) {
    goto ERROR;
  }
  if (stored_ss != 32766) {
    goto ERROR;
  }

  const unsigned short us = 65535;
  int shifted_us = us >> 1;
  unsigned short stored_us = (unsigned short)shifted_us;
  if (shifted_us != 32767) {
    goto ERROR;
  }
  if (stored_us != 32767) {
    goto ERROR;
  }

  unsigned int ui = 0x7FFFFFFFU;
  unsigned long widened = ui << 1;
  if (widened != 0xFFFFFFFEUL) {
    goto ERROR;
  }

  return 0;

ERROR:
  return 1;
}
