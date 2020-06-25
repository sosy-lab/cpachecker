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
	read_unlock(rwlock_1);
	read_unlock(rwlock_1);
	read_unlock(rwlock_1);
	// more read unlocks
	

	ldv_check_final_state();
}

