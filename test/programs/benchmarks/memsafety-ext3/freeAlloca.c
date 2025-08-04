typedef unsigned int size_t;
extern void *alloca(size_t size);
extern void free(void*);
extern char __VERIFIER_nondet_char(void);

int main(void){
    char *p = alloca(10 * sizeof(char));

    for (int i = 0; i < 10; i++) {
        p[i] = __VERIFIER_nondet_char();
    }

    if (p[2] == 'a')
        free(p);

    return 0;
}
