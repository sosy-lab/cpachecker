int areNatural(int *numbers){
    int i = 0;
    while(i < 10) {
        if(numbers[i] <= 0){
            return 0;
        }
        i++;
    }
    return 1;
}

int main(void) {
    char numbers[] = {0,1,2,3,4,5,6,7,8,9};
    // numbers is array of chars, but should be integers
    int result = areNatural((int*) numbers);
    return 0;
}
