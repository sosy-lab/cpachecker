#ifndef REAL_HEADERS
  #include "cpalien-headers.h"
#else
  #include <stdio.h>
  #include <stdlib.h>
#endif

int *do_an_allocation() {
	int *a = malloc(sizeof(int));
	int *b = a;
	*a = 4;
	return b;
}

void free_an_allocation(void *fr){
  free(fr);
}

int main(int argc, char* argv[]){
	int *ptr  = do_an_allocation();
  free_an_allocation(ptr);
  free_an_allocation(ptr);
	return 0;
}
