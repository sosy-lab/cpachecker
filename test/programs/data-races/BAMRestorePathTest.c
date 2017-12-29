int global = 0;

int g() {
  return 0;
}

void* f() {
    void *res;
    g();
    global = 0;
    return res;
}

int ldv_main() {
    int tmp = 0;
    
    f();
    
    g();
    
    f();
}
