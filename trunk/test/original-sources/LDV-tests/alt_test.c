#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

int globalState = 0;
void* l_malloc(int);
void l_free(void*);

int
main(int argc, char* argv[]) {
	int *a = (int*) l_malloc(sizeof(int));
	l_free(a);
	l_free(a);
	return 0;
}

void* l_malloc(int size) {	
	void *retVal = malloc(size);
	if(retVal != NULL) 
		globalState=1; 
	return retVal;
}

void l_free(void* ptr) {
	/* если вызывается free на указатель, на котором уже
	 * была вызвана функция free, то результат работы неопределен,
	 * поэтому поворный вызов free недопустим */
	if(ptr!=NULL) assert(globalState == 1);
	globalState = 0;
	free(ptr);
}
