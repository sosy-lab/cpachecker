typedef unsigned long size_t;
struct S
{
	int a1;
	int a2;
};
extern void * malloc(size_t __size);

int __VERIFIER_nondet_int();

int arr[5];

int main()
{
	struct S* s1 = (struct S*) malloc(sizeof(struct S));
	arr[s1->a2] = s1->a1;
	if (s1->a2 == 1 && s1->a1 != arr[1]) {
ERROR:
		return 1;
	}
	return 0;
}
