// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef struct {
	unsigned int break_lock;
	unsigned int magic, owner_cpu;
	void *owner;
} rwlock_t;

int __VERIFIER_nondet_int(void);

extern void read_lock(rwlock_t *);
extern void write_lock(rwlock_t *);
extern void read_unlock(rwlock_t *);
extern void write_unlock(rwlock_t *);
extern int read_trylock(rwlock_t *);
extern int write_trylock(rwlock_t *) {
	return __VERIFIER_nondet_int();
}

void ldv_check_final_state(void);

void main(void)
{
	rwlock_t *rwlock_1;
	
	read_lock(rwlock_1);
	read_lock(rwlock_1);
	write_lock(rwlock_1);
	read_lock(rwlock_1);
	read_unlock(rwlock_1);
	write_unlock(rwlock_1);
	read_unlock(rwlock_1);
	read_unlock(rwlock_1);
	// checking state R1_W1

	ldv_check_final_state();
}

