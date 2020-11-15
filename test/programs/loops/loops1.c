// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main() {
	int x;
	int p1;
	int p2;
	int p3;

	if (p1) {
L1a:	/* beginning a of loop 1 */
		x = 0;
	} else {
L1b:	/* beginning b of loop 1 */
		x = 1;
	}

	while (0) { /* loop 2 */
		if (p2) {
			goto L2;
		}
	}
L2:	/* end of loop 2 */

	if (p3) {
		goto L1a; /* end of loop 1 */
	} else {
		goto L1b; /* end of loop 1 */
	}
}
