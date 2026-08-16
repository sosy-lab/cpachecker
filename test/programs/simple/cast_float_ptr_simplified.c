// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2014 Antoine Mine
// SPDX-FileCopyrightText: 2014-2025 The SV-Benchmarks Community
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This is a highly simplified version of float-benchs/cast_float_ptr.c
// in SV-Benchmarks.

int main() {
  double d = 0.0/0.0;
  unsigned int i = *((unsigned*)&d);
  if ( (i & 0x7FF00000) == 0x7FF00000 ) return 0;

  if (d >= 0 || d <= 0) {
    return 0;
  } else {
ERROR:
    return 1;
  }
}
