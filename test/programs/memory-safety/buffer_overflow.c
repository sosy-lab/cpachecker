#include <string.h>
int main() {
    char buffer[8];
    strcpy(buffer, "This string is too long"); // Buffer overflow
    return 0;
}
