int counter = 0;
extern void* unknown_func();

void* f() {
    void *res;
    res = unknown_func();
    
    return res;
}

int main() {
    int tmp = 0;
    
    f();
    
    if (counter != 0) {
        ERROR: goto ERROR;
    }
    
    if (tmp != 0) {
        goto ERROR;
    }
}
