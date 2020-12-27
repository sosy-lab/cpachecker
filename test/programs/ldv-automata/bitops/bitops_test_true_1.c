// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

static inline int find_next_bit(const unsigned long *vaddr, int size, int offset);

static inline int find_first_bit(const unsigned long *vaddr, unsigned size) {
	int res;
	return res < size ? res : size;
}

void main(void)
{
	unsigned long size, offset, res_1, res_2;
	const unsigned long *addr;

	// check values
	res_1 = find_next_bit(addr, 10, 5);
	res_1 = find_next_bit(addr, 0, 0);

	// check that size >= offset
	if (size < offset)
		return;
	res_1 = find_next_bit(addr, size, offset);
	
	// check return value of find_next_bit
	res_2 = find_next_bit(addr, size, res_1);
	
	// check return value of find_first_bit
	res_1 = find_first_bit(addr, size);
	res_2 = find_next_bit(addr, size, res_1);
}

