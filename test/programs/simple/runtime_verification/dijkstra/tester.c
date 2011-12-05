#include <time.h>
#include <stdio.h>
#include <stdlib.h>

int nondet_int() {
	return rand() % 2;
}

time_t last_measure = 0;
long long operation_counter = 0;

int performed_operation() {
	operation_counter++;
	if(time(NULL) - last_measure > 1) {		
		printf("OPERATIONS PER SEC: %lli \n", operation_counter);
		last_measure = time(NULL);
		operation_counter = 0;
	}
}

int tmp;

#include "instrumented.c"

int main() {
	return entry();
}
