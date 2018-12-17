typedef int pthread_mutex_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;

int gvar;
pthread_mutex_t mutex;

void unsafe_func(void) {
	gvar = 1;
	return 0;
}

void safe_func(void) {
	int b;
	pthread_mutex_lock(&mutex);
	b = gvar;
	pthread_mutex_unlock(&mutex);
}

void main(void) {
	unsafe_func();
	safe_func();
	return;
}
