extern void __VERIFIER_mark_tainted(int);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
	int a, b, c, d;
	for(a = 0; a < 10; a++) {
		printf("value of a: %d\n", a);
	}
	if(a < 10) {
		__VERIFIER_mark_tainted(b);
	}
	else if(a > 1)
	{
		__VERIFIER_mark_tainted(c);
	}
	else {
		__VERIFIER_mark_tainted(d);
	}
	__VERIFIER_assert_untainted(a);
	__VERIFIER_assert_untainted(b);
	__VERIFIER_assert_tainted(c);
	__VERIFIER_assert_untainted(d);
	return 0;
}
