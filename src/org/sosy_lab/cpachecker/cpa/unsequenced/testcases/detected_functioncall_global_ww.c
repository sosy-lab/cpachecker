#include <stdio.h>

//rewrite example in "An Executable Formal Semantics of C with Applications"
char global_char;

int a(int x, int y) {
    global_char = 'a';
    printf("%c", global_char);
    return 0;
}

int b() {
    global_char = 'b';
    printf("%c", global_char);
    return 0;
}

int c(int x) {
    global_char = 'c';
    printf("%c", global_char);
    return 0;
}

int d() {
    global_char = 'd';
    printf("%c", global_char);
    return 0;
}

void e(int x) {
    global_char = 'e';
    printf("%c", global_char);
}

// f() prints "f" and returns function pointer to e()
void (*f())(int) {
    global_char = 'f';
    printf("%c", global_char);
    return e;
}

int main() {
    // Execute f() which returns e(), then call e() with result of a(b(), c(d()))
    f()(a(b(), c(d())));
    return 0;
}
