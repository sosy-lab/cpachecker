// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int x = 1;
    // assume that some operation made x to be secret/tainted
    __VERIFIER_set_public(x,0);
    // assertion violation/failure expected
    __VERIFIER_is_public(x,1);
}
