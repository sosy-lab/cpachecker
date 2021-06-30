extern int getchar();
extern int scanf();
extern int gets();
extern int fopen();

extern void printf(int);
extern void strcmp(int);
extern void fputs(int);
extern void fputc(int);
extern void fwrite(int);


extern void __VERIFIER_mark_tainted(int);
extern void __VERIFIER_mark_untainted(int);
extern void __VERIFIER_assert_untainted(int);
extern void __VERIFIER_assert_tainted(int);

int main(void) 
{
    int a; int b = 5; int c;
    __VERIFIER_mark_tainted(a);
    b = a;
    c = b + 0;
    c = 5;
    __VERIFIER_assert_tainted(a);
    __VERIFIER_assert_tainted(b);
    __VERIFIER_assert_untainted(c);
    
    return 0;
}