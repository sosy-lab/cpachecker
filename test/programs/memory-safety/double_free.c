#include <stdlib.h>
int main() {
    int *ptr = malloc(sizeof(int));
    free(ptr);
    free(ptr); // Double free
    return 0;
}