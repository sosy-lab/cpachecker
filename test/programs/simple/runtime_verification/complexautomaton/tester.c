#include <time.h>
#include <stdio.h>
#include <stdlib.h>

int sum; //avoid optimization

int anti_op() {
	sum = sum + 1;
}

int nondet_int() {
	return rand() % 2;
}

#include "instrumented.c"

#define ITERATIONS 10000000

int main() {
	time_t start = time(NULL);

	sum = 0; 
	
	for(int i = 0; i < ITERATIONS; i++) {
		entry();		
		sum += i;
	}	
	time_t diff_time = time(NULL) - start;

	printf("%d took %lds (sum=%d)\n", ITERATIONS, diff_time, sum);
}
