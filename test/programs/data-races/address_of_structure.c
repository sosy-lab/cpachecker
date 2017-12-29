struct my_struct {
  int a;
};

int g(int *a) {
	*a = 1;
	return 0;
}

int f(struct my_struct *A) {
	return g(&A->a);
}

int main() {
  struct my_struct S;
  
  f(&S);
}
