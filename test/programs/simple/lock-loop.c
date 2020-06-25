// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int L = 0;

void lock() {
	if (L != 0) {
ERROR:
		goto ERROR;
	}
	L++;
}

void unlock() {
	if (L != 1) {
ERROR:
		goto ERROR;
	}
	L--;
}

int main() {
	int old, new;
	int undet;
	do {
		lock();
		old = new;
		if (undet) {
			unlock();
			new++;
		}
	} while (new != old);
}
