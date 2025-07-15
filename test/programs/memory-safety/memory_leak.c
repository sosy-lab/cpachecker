#include <stdlib.h>
int main() {
    int *ptr = malloc(sizeof(int));
    // Memory allocated but not freed (leak)
    return 0;
}