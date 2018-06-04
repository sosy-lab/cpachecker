typedef int pthread_mutex_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int __VERIFIER_nondet_int();

int gvar;
pthread_mutex_t mutex;

void inner(void) {
	int b = __VERIFIER_nondet_int();
	pthread_mutex_lock(&mutex);
	gvar = 10;
	pthread_mutex_unlock(&mutex);
}

void main(void) {
	inner();
}
