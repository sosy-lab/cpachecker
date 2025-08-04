typedef unsigned int size_t;
extern  void free(void*);
extern void* malloc(size_t);
extern void* realloc( void *ptr, size_t new_size );
extern char __VERIFIER_nondet_char(void);

int main(void) {
    char* p = malloc(10 * sizeof(int));

    for (int i = 0; i < 10; i++) {
        p[i] = __VERIFIER_nondet_char();
    }

    free(p);

    if (p[2] == 'a')
        p = realloc(p, 20 * sizeof(int));

    return 0;
}
