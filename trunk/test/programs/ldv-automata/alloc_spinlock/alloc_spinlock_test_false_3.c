// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned gfp_t;
typedef unsigned int size_t;
struct usb_device;
struct usb_interface;

typedef struct spinlock {
	union {
		void* rlock;
		struct {
			int __padding[128];
		};
	};
} spinlock_t;

typedef struct {
	int counter;
} atomic_t;


int some_func(spinlock_t *lock);

static inline void spin_lock(spinlock_t *lock);
static inline void spin_unlock(spinlock_t *lock);
static inline int spin_trylock(spinlock_t *lock) {
    return some_func(lock);
}
static inline int spin_is_locked(spinlock_t *lock);
extern int _atomic_dec_and_lock(atomic_t *atomic, spinlock_t *lock);

void free(void *);
static void *kmalloc(size_t size, gfp_t flags);
void *vmalloc(unsigned long size);
void kfree(const void *objp) {
	free(objp);
}

static void memory_allocation(gfp_t flags)
{
	size_t size;
	void *mem = kmalloc(size, flags);
	kfree(mem);
}

static void memory_allocation_nonatomic(void)
{
	int size;
	void *mem = vmalloc(size);
	kfree(mem);
}

void main(void)
{
	spinlock_t *lock_1;
	atomic_t *atomic;

	if (_atomic_dec_and_lock(atomic, lock_1)) {
		// cannot be called from spinlock section
		memory_allocation_nonatomic();
		spin_unlock(lock_1);
	}
}

