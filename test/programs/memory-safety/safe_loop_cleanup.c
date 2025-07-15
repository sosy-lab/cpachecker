#include <stdlib.h>
int main() {
    int loop_bound = 10;
    int *ptr_array[10];
    for (int i = 0; i < loop_bound; i++) {
        ptr_array[i] = malloc(sizeof(int));
    }
    for (int i = 0; i < loop_bound; i++) {
        free(ptr_array[i]);
    }
    return 0;
}