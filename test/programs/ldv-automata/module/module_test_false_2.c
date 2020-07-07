// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef int bool;
struct module;

static inline bool try_module_get(struct module *module);
static inline void module_put(struct module *module)
{
}
static inline void __module_get(struct module *module)
{
}
extern void module_put_and_exit(struct module *mod, long code);
int module_refcount(struct module *mod);

void ldv_check_final_state(void);

const int N = 10;

void main(void)
{
	struct module *test_module_1;
	struct module *test_module_2;
	int i;

	for (i = 0; i < N; i++) {
		__module_get(test_module_1);
	}
	// more decrements
	for (i = 0; i < N + 1; i++) {
		module_put(test_module_1);
	}
	
	ldv_check_final_state();
}

