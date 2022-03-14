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
	rwlock_t *rwlock_2;
	rwlock_t *rwlock_3;
	rwlock_t *rwlock_4;

	write_lock(rwlock_1);
	write_unlock(rwlock_1);

	read_lock(rwlock_1);
	read_lock(rwlock_1);

	write_lock(rwlock_2);
	write_unlock(rwlock_2);

	read_lock(rwlock_2);
	read_lock(rwlock_3);

	write_lock(rwlock_2);
	write_unlock(rwlock_2);

	read_unlock(rwlock_3);
	read_unlock(rwlock_2);
	read_unlock(rwlock_1);
	read_unlock(rwlock_1);

	write_lock(rwlock_3);
	write_unlock(rwlock_3);

	if (read_trylock(rwlock_1)) {
		if (write_trylock(rwlock_1)) {
			write_unlock(rwlock_1);
		}
		if (read_trylock(rwlock_2)) {
			if (read_trylock(rwlock_3)) {
				if (read_trylock(rwlock_4)) {
					if (write_trylock(rwlock_1)) {
						write_unlock(rwlock_1);
					}
					read_unlock(rwlock_4);
					if (write_trylock(rwlock_4)) {
						write_unlock(rwlock_4);
					}
				}
				read_unlock(rwlock_3);
				if (write_trylock(rwlock_3)) {
					write_unlock(rwlock_3);
				}
			}
			read_unlock(rwlock_2);
		}
		read_unlock(rwlock_1);
	}
	
	read_lock(rwlock_1);
	read_lock(rwlock_1);
	write_lock(rwlock_1);
	write_unlock(rwlock_1);
	read_unlock(rwlock_1);
	read_unlock(rwlock_1);
	
	if (read_trylock(rwlock_1)) {
		if (write_trylock(rwlock_1)) {
			write_unlock(rwlock_1);
		}
		if (read_trylock(rwlock_2)) {
			if (read_trylock(rwlock_3)) {
				if (read_trylock(rwlock_4)) {
					if (write_trylock(rwlock_1)) {
						write_unlock(rwlock_1);
					}
					read_unlock(rwlock_4);
					if (write_trylock(rwlock_4)) {
						write_unlock(rwlock_4);
					}
				}
				read_unlock(rwlock_3);
				if (write_trylock(rwlock_3)) {
					write_unlock(rwlock_3);
				}
			}
			read_unlock(rwlock_2);
		}
		read_unlock(rwlock_1);
	}
	
	int i = 0;
	for (i = 0; i < 10; i++)
		read_lock(rwlock_1);
	write_lock(rwlock_1);
	write_unlock(rwlock_1);
	for (i = 0; i < 10; i++)
		read_unlock(rwlock_1);

	ldv_check_final_state();
}

