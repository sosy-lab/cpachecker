//Test should check, how the value analysis handles loops
int global;

int g(int a) {
    return a + 1;
}

int main() {
  int i = 0;
  int res = 0;
  for (i = 0; i < 10000; i++) {
      res = g(res);
  }
  if (res < 10000) {
    global = 0;
  }
}

int ldv_main() {
    global = 1;
	main();
}
