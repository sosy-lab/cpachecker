
void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

/*
 * We consider the following program as unsafe, because the possibility of the
 * error occuring can not be ruled out.
 * It is not possible to give a general range for which the problem can not occur.
 * Valid ranges could be set with a memory model.
 */
void main() {
  int a = 125;
  int* p = &a;
  test(p != *p);
}

