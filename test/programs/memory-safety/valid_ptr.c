#include <stdlib.h>

int main() {
    int *ptr = malloc(sizeof(int));
    if (ptr != NULL) {
        *ptr = 42;
        free(ptr);
    }
    return 0;
}
