#include <stdio.h>
int *global_ptr;
void foo() {
    int local = 42;
    global_ptr = &local;
}
int main() {
    foo();
    int value = *global_ptr; // Dangling pointer dereference
    printf("%d\n", value);
    return 0;
}