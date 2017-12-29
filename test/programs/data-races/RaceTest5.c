int global = 0;
int f() {
  int a, b;
  if (a == 0) {
	  b++;
	  if (a != 0) {
		  intLock();
		  global = 1;
		  intUnlock();
		  ERROR:
		  goto ERROR;
	  }
  }
}

int ldv_main() {
	f();
}

