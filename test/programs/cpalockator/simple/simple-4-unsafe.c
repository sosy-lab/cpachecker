typedef int pthread_mutex_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int __VERIFIER_nondet_int();

int gvar;
pthread_mutex_t mutex;

int func(void) {
	gvar = 1;
	return 0;
}

void main(void) {
	func();
	pthread_mutex_lock(&mutex);
	func();
	pthread_mutex_unlock(&mutex);
	func();
}
