struct my_struct {
	int a[2];
} A;

int safe;
int unsafe;

void control_function(void *arg) {
    f();
}

int f() {
    safe = 1;
    unsafe = 1;
}

struct pthread {
    int tmp;
};

typedef struct pthread pthread_t;

int main() {
    int *a;
    pthread_t thread_arg;
	safe = 0;
	A.a[0] = 1;
	pthread_create(&thread_arg, 0, &control_function, 0);
    unsafe = 0;
}
