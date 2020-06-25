int counter = 0;
extern void* unknown_func();

void assume_equality(void* v) {
    if (v != 0) {
        counter++;
    }
}

void* f() {
    void *res;
    res = unknown_func();
    
    assume_equality(res);
    
    return res;
}

int main() {
    void* tmp;
    
    tmp = f();
    
    if (tmp == 0) {
        return 0;
    }
    
    if (counter == 0) {
        ERROR: goto ERROR;
    }
}
