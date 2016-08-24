
void *kzalloc(unsigned long size);

void __VERIFIER_error(void) { ERROR: goto ERROR; }

void *zzzalloc(unsigned long size) {
   return kzalloc(size);
}

struct wrapper { void *f1, *f2; };

void *zzalloc(unsigned long size1, unsigned long size2, int flag) {
	void * result1 = zzzalloc(size2), *result2 = zzzalloc(size1);
	unsigned long i;
	i = (unsigned long)&i + (unsigned long)result1 + (unsigned long)result2;
	if (i < 0) {
		__VERIFIER_error();
	}
	void *tmp;
	if (flag) {
		tmp = result1;
		result1 = result2;
		result2 = tmp;
        }
	struct wrapper *result = zzzalloc(sizeof(struct wrapper));
	if (!result) { while (1); }
	result->f1 = result1;
	result->f2 = result2;
	return result;
}

void *zalloc(unsigned long size1, unsigned long size2) {
	void *result = zzalloc(size1, size2, 0);
	return result;
}

struct arr1 { int arr[30]; };
struct arr2 { char arr[30]; };

int main() {
	int i = 0;
	struct wrapper *w = zalloc(30, 10);
        struct arr2 *arr2 = w->f2;
        struct arr1 *arr = w->f1; 
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
