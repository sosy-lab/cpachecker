int b = 0;

int ldv_main() {
 int *a;
 if (a > 0)
 {
  a = 0;
 }else
 {
  a = &b;
 }
 *a = 1;
 return 0;
}