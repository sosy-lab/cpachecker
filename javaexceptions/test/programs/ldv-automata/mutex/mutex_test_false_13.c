// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct mutex;
struct kref;
typedef struct {
	int counter;
} atomic_t;

int __VERIFIER_nondet_int(void);

extern void mutex_lock(struct mutex *lock);
extern void mutex_lock_nested(struct mutex *lock, unsigned int subclass);
extern int mutex_lock_interruptible(struct mutex *lock);
extern int mutex_lock_killable(struct mutex *lock);
extern int mutex_lock_interruptible_nested(struct mutex *lock, unsigned int subclass);
extern int mutex_lock_killable_nested(struct mutex *lock, unsigned int subclass);
static inline int mutex_is_locked(struct mutex *lock);
extern int mutex_trylock(struct mutex *lock);
extern void mutex_unlock(struct mutex *lock);
extern int atomic_dec_and_mutex_lock(atomic_t *cnt, struct mutex *lock);
static inline int kref_put_mutex(struct kref *kref,
				 void (*release)(struct kref *kref),
				 struct mutex *lock);

static void specific_func(struct kref *kref);

void ldv_check_final_state(void);

void main(void)
{
	struct mutex *mutex_1, *mutex_2, *mutex_3, *mutex_4, *mutex_5;
	struct kref *kref;
	atomic_t *counter;
	
	int res = atomic_dec_and_mutex_lock(counter, &mutex_1);
	// missing return value check
	mutex_unlock(&mutex_1);
	

	ldv_check_final_state();
}

