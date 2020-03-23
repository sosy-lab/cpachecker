#include <stdlib.h>
#include <stdio.h>

static void shell_sort(int a[], int size) {
	int i, j;
	int h = 1;
 	do {
		h = h*3+1;
 	} while (h <= size);
 
	do {
		h /= 3;
		for(int i = h; i < size; i++) {
			int v = a[i];
			for (j = i; j >= h && a[j-h] > v; j-=h)
				a[j] = a[j-h];
			if (i != j) a[j] = v;
		}
	} while (h!=1);
}

int main(int argc, char* argv[]){
	int i = 0;
	int *a = NULL;
	a = (int*)malloc((argc-1) * sizeof(int));
	for (i = 0; i < argc-1; i++)
		a[i] = atoi(argv[i+1]);
	shell_sort(a, argc);
	if(a[0] == 0){
		free(a);
		goto ERROR;
	}
	free(a);
EXIT: return 0;
ERROR: return 1;
}

