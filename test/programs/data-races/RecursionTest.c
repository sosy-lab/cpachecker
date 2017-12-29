/* Test for handling recursion*/
int global;
void splnet();

int a() {
    int c;
    if (c) {
       b();
    }
}

int b() {
    int c; 
    a();
    
    global = 0;
}

int ldv_main() {
	a();
}
