extern void __VERIFIER_mark_tainted(void const *);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void){
    int a;
    int *b = &a;

    __VERIFIER_mark_tainted(a);

    int c = *b;

    __VERIFIER_assert_tainted(a);
    __VERIFIER_assert_tainted(c);

	return 0;
}
