#ifndef REAL_HEADERS
  #include "cpalien-headers.h"
#else
  #include <stdio.h>
  #include <stdlib.h>
#endif

int do_an_allocation() {
	int *a = malloc(sizeof(int));
	int *b = a;
	free(a);
	*b = 4;

	return 48;
}

int main(int argc, char* argv[]){
	int i = do_an_allocation();
	return i % 2;
}
