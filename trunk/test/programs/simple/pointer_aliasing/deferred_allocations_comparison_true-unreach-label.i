
void *kzalloc(unsigned long size);

void __VERIFIER_error(void) { ERROR: goto ERROR; }

void *zzzalloc(unsigned long size) {
   return kzalloc(size);
}

struct mediator { void *val; } med;


void *zzalloc(unsigned long size) {
	void * result = zzzalloc(size);
	unsigned long i;
	i = (unsigned long)&i + (unsigned long)result;
	if (i < 0) {
		__VERIFIER_error();
	}
	med.val = result;
	return &med;
}

void *zalloc(unsigned long size) {
	void *result = zzalloc(size);
	return ((struct mediator*)result)->val;
}

struct arr1 { int arr[30]; };
struct arr2 { char arr[30]; };

struct wrapper { void *f1, *f2; };

int main() {
	int i = 0;
        struct wrapper w;
	w.f1 = zalloc(30);
	w.f2 = zalloc(10);
        struct arr1 *arr;
        if (arr != w.f1) { while(1); };
        struct arr2 *arr2;
	if (arr2 != w.f2) { while(1); }; 
	if (!arr || !arr2) return 0;
	while (i < 2) {
		arr->arr[i] = i;
		i++;
	}
	
	i = 2;
        while (i >= 0) {
		arr2->arr[i] = -i;
		i--;
	}
	
	if (arr->arr[0] != 0) {
		__VERIFIER_error();
	}
	return 0;
}
