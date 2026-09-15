// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// testing upper and lower bounds of signed long long
enum e1 {
    E1,
    E2,
    // upper bound of lld - 1
    E3 = 9223372036854775806L,
    // upper bound of lld
    E4,
    // moving within lld positive space
    E5 = E4 - 1,
    // lower bound of lld
    E6 = -9223372036854775807L - 1,
    // moving within lld negative space
    E7,
    // calculating with upper and lower bound of lld
    E8 = E4 + E6
};

int main() {
    if (E1 != 0) {
        goto ERROR;
    }
    if (E2 != 1) {
        goto ERROR;
    }
    if (E3 != 9223372036854775806L) {
        goto ERROR;
    }
    if (E4 != 9223372036854775807L) {
        goto ERROR;
    }
    if (E5 != 9223372036854775806L) {
        goto ERROR;
    }
    if (E6 != -9223372036854775807L - 1) {
        goto ERROR;
    }
    if (E7 != -9223372036854775807L) {
        goto ERROR;
    }
    if (E8 != -1){
        goto ERROR;
    }

  return 0;

ERROR:
  return 1;
}
