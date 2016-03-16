typedef unsigned long size_t;
struct S
{
	int a1;
	int a2;
};
extern void * malloc(size_t __size);

int main()
{
	struct S* s1 = (struct S*) malloc(sizeof(struct S));
	s1->a2 = s1->a1;
	if (s1->a1 != s1->a2) {
ERROR:
		return 1;
	}
	return 0;
}
