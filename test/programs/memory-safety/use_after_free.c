#include <stdlib.h>
int main() {
    int *ptr = malloc(sizeof(int));
    free(ptr);
    *ptr = 42; // Use after free
    return 0;
}
