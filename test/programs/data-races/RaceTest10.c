//#include <stdio.h>
int false_unsafe;
int unknown;

int f() {
	int b = 0;
	b = unknown;
	if (b) {
		kernDispatchDisable();
		false_unsafe = 1;
		kernDispatchDisable();
	}
}

int ldv_main() {
	false_unsafe = 0;
	f();
}
