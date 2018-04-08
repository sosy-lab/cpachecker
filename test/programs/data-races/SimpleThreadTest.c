struct pthread {
    int tmp;
};

typedef struct pthread pthread_t;

struct ldv_thread {
    int n;
    pthread_t **threads;
};

int safe;
int unsafe;

/*struct thread *ldv_thread_create(void *(*start_routine) (void *), void *arg) {
    (*start_routine)(arg);
}*/

void control_function(void *arg) {
    f();
}

int f() {
    safe = 1;
    unsafe = 1;
}

int ldv_main() {
    int *a;
	safe = 0;
	ldv_thread_create(a, control_function);
    unsafe = 0;
}
