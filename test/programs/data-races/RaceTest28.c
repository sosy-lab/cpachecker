//Test should check, that there are no global predicates in refinement, even through local assignements
int global;
int true_unsafe;
int test_unsafe;

int f() {
  int b = global;
  if (b == 0) {
	  intLock();
  }
  b = global;
  if (b == 0) {
	  test_unsafe = 2;
  }
  if (b == 0) {
	  intUnlock();
  }
} 

int main() {
  true_unsafe = 1;
  intLock();
  test_unsafe = 2;
  intUnlock();
  f();
}

int ldv_main() {
	main();
}
