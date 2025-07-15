#include <stdlib.h>
typedef struct {
    char *str1;
    size_t size1;
    char *str2;
    size_t size2;
} MyStruct;

int validStruct(MyStruct *p) {
    if (!p) return 0;
    if (!p->str1 || !p->str2) return 0;
    // Invariant checks would depend on ACSL or runtime
    return 1;
}

int main() {
    MyStruct *struct_ptr = malloc(sizeof(MyStruct));
    struct_ptr->str1 = malloc(32);
    struct_ptr->size1 = 32;
    struct_ptr->str2 = malloc(48);
    struct_ptr->size2 = 48;
    validStruct(struct_ptr);
    free(struct_ptr->str1);
    free(struct_ptr->str2);
    free(struct_ptr);
    return 0;
}