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


int ldv_thread_join(void *(*start_routine) (void *), pthread_t *thread) {
    //??
}

void* control_function(void *arg) {
    f();
}

int f() {
    safe = 1;
    unsafe = 1;
}

int ldv_main() {
    int *a;
	ldv_thread_create(a, control_function);
    unsafe = 0;
    ldv_thread_join(a, control_function);
    safe = 1;
}
