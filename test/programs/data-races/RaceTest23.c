/* There was a bug with consider the function calls as unsafes
 */
int unsafe;

int g() {
    unsafe = 1;
}

int f() 
{ 
    g();
}
int ldv_main() {
    int undef;
    switch (undef) {
        case 0:
        f();
        break;
        case 1:
        g();
        break;
    }
}

