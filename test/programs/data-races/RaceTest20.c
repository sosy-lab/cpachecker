/* This test checks the handling of write and read usages
 * false_unsafe is only read and isn't an unsafe
 * unsafe is both read and written in different functions
 * global is read and written in the same function
 */
int false_unsafe;
int unsafe;
int global;

int f() 
{ 
  unsafe = false_unsafe;
}

int g() {
  unsafe = global;
  global = false_unsafe;
}

int ldv_main() {
    f();
    g();
}

