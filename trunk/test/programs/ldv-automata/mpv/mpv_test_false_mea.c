// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * This program violates 3 automata specifications:
 *  - alloc_spinlock (1 bug);
 *  - spinlock (2 bugs);
 *  - rculock (indeterminate number of bugs)
 * Specification mutex is relevant.
 */
typedef unsigned gfp_t;
typedef unsigned int size_t;
typedef struct spinlock {
	union {
		void* rlock;
		struct {
			int __padding[128];
		};
	};
} spinlock_t;

struct mutex;

void *__VERIFIER_nondet_pointer(void);
int __VERIFIER_nondet_int(void);
void ldv_check_final_state(void);

extern void rcu_read_lock(void);
extern void rcu_read_unlock(void);
extern void rcu_read_lock_bh(void);
extern void rcu_read_unlock_bh(void);
static inline void spin_lock(spinlock_t *lock);
static inline void spin_unlock(spinlock_t *lock);
extern void mutex_lock(struct mutex *lock);
extern void mutex_unlock(struct mutex *lock);
extern void free(void *);
extern void *kmalloc(size_t size, gfp_t flags);
extern void kfree(const void *objp) {
	free(objp);
}
extern void function_with_memory_allocation(gfp_t flags)
{
	size_t size;
	void *mem = kmalloc(size, flags);
	kfree(mem);
}

void main(void)
{
	spinlock_t *lock_1 = (spinlock_t *)__VERIFIER_nondet_pointer();
	spinlock_t *lock_2 = (spinlock_t *)__VERIFIER_nondet_pointer();
	struct mutex *mutex_1 = (struct mutex *)__VERIFIER_nondet_pointer();
	struct mutex *mutex_2 = (struct mutex *)__VERIFIER_nondet_pointer();
	gfp_t good_flag = 32;
	gfp_t bad_flag = __VERIFIER_nondet_int();
	int x = __VERIFIER_nondet_int();

	mutex_lock(&mutex_1);
	function_with_memory_allocation(good_flag);
	function_with_memory_allocation(bad_flag);
	spin_lock(lock_1);
	function_with_memory_allocation(good_flag);
	function_with_memory_allocation(bad_flag); // first bug for alloc_spinlock specification
	spin_unlock(lock_1);
	function_with_memory_allocation(good_flag);
	function_with_memory_allocation(bad_flag);
	mutex_lock(&mutex_2);
	if (x) {
		spin_lock(lock_1);
		//spin_unlock(lock_1); // first bug for spinlock specification
	} else {
		//spin_lock(lock_2); // second bug for spinlock specification
		spin_unlock(lock_2);
	}
	mutex_unlock(&mutex_2);

	while (1) {
		int i = __VERIFIER_nondet_int();
		if (i == 0) {
			rcu_read_lock();
		}
		else if (i == 1) {
			rcu_read_unlock();
		} else {
			break;
		}
	}

	mutex_unlock(&mutex_1);

	ldv_check_final_state();
}

