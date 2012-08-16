
void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

/** Tests tracking of pointers over several degrees of indirection */
void main() {
  int a = 125;
  int* p = &a;
  test(p != *p);
}

