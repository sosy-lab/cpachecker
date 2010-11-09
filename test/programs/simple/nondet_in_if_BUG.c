int g = 0;

void f() {
  int __BLAST_NONDET ;

  if (__BLAST_NONDET == 0) {
    g = 0;
  } else {
    if (__BLAST_NONDET == 1) {
      g = 1;
    } else {
      g = 2;
    }
  }
}

int main(void) {
  f();
  if (0 != g) {
ERROR:
	goto ERROR;
  }
}
