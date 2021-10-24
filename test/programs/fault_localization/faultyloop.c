// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

        // algorithm should square "val"
        int val = 3;
        int i = 1;
        int v = 0;
        int res = 0;
        while(v < val) { // FIX: i < val
                v = v + 2*i + 1;
                i++;
        }
        res = v + 1;
	if (val * val == res)
		goto EXIT;
	else
		goto ERROR;

EXIT: return 0;
ERROR: return 1;
}
