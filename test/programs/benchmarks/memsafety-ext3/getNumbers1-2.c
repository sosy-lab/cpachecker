extern int printf (const char* format, ...);

// function returns array of numbers
int* getNumbers(void) {

    static int array[10]; // array is static, which is correct

    for (int i = 0; i < 10; ++i) {
       array[i] = i;
    }

    return array;
}

int main(void) {

    int *numbers = getNumbers();

    for (int i = 0; i < 10; i++ ) {
       printf( "%d\n", *(numbers + i));
    }

    return 0;
}
