int nondet();

int main() {
    int a = nondet();
    int b = a - a;
    if(b != 0) {
        goto Error;
    }
    
    return 0;

    Error:
    return -1;
}


