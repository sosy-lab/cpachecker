// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int _a = 1, _b = 2, c;
    // _a and _b inside the statement expression are initialized with themselves
    // and not with _a and _b from the main function
    c = ({int _a = _a, _b = _b; _a > _b ? _a : _b; });
    
    // the value of c is undefined, therefore the label should be reachable
    if (c != _a && c != _b) {
        ERROR: // reachable
            return 1;
    }

    return 0;
}
