// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <malloc.h>
#include <assert.h>

int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

struct path_info {
	int list;
};

void list_add(int *new) {
	assert(new!=NULL);
}

static void rr_fail_path(struct path_info *pi)
{
	list_add(&pi->list);
}


int main(void) {
	struct path_info pi;
	rr_fail_path(&pi);
}
