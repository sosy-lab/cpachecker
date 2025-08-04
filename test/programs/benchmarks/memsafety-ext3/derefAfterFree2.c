typedef unsigned int size_t;
extern  void free(void*);
extern void* malloc(size_t);

int main(void) {
    int* p = malloc(10 * sizeof(int));

    for(int i = 0; i < 10; i++) {
        p[i] = 1;
        if(i == 5) {
            free(p);
        }
    }

    return 0;
}
