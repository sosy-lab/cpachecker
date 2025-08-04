typedef unsigned int size_t;
extern  void free(void*);
extern void* malloc(size_t);

void freePointer(int* p) {
    free(p);
}

int main(void) {
    int* p = malloc(10 * sizeof(int));

    freePointer(p);

    p[0] = 1;

    return 0;
}
