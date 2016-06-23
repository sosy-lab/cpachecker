
int nondet() {
	int a;
	return a;
}

int foo() {
	return 0;
}

extern int bar();

static int main() 
{ 
    agg();
    agg();
}

static int agg() {

  again: 

  ldv_34622: ;

  int a = nondet();
  if (a) {    

    int b = nondet();
    if (b) {
      return (-1);
    }

    return (0);
  }

  int c = nondet();
  if (c) {
    goto ldv_34620;
  }

  goto ldv_34622;
  ldv_34620: 

  int d = nondet();
  if (d) {
    goto again;
  }
  return (-5);
}



