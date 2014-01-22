# 1 "files/callfpointer.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "files/callfpointer.c"





void f(void g(int)) {
 g(1);
}

void h(int i) {
 if(i==1) {
  ERROR: goto ERROR;
 } else {

 }
}

void z(int i) {}

int main(void) {
 void (*h)(int) = &z;

 // in the next lines, h references the local variable
 f(h);
 h(1);

 return 0;
}
