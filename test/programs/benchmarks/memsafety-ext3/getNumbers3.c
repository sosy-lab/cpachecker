int array[10];

// function returns array of numbers
int* getNumbers() {
    for (int i = 0; i < 10; ++i) {
        array[i] = i;
    }

    return array;
}

int main (void) {
    int *numbers = getNumbers();
    numbers[0] = 100;
    return 0;
}
