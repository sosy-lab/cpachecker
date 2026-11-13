// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// The value analysis does not track a variable whose address is taken,
// so only the SMG analysis can execute this loop.
int first_byte(const unsigned char *k) { return k[0]; }

int main(void) {
  int s = 0;
  for (int i = 0; i < 3; i++) {
    s += first_byte((const unsigned char *)&i);
  }
  return s == 3 ? 0 : 1;
}
