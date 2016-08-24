# 1 "./pointer_reflection.UNSAFE.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "./pointer_reflection.UNSAFE.c"

void test(int x) {
 if (!x) {
ERROR: goto ERROR;
 }
}







void main() {
  int a = 125;
  int* p = &a;
  test(p != *p);
}
