void f(void g(int), int i) {
 if (i == 0) {
   ERROR:
   return;
 }
 g(g, i-1);
}

int main(void) {
 f(f, 3);

 return 0;
}
