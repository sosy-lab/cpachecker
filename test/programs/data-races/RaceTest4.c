//Test should check, that there are no global predicates in refinement
int global;
int true_unsafe;
int true_unsafe2;
int true_unsafe3;
int true_unsafe4;

struct A {
	int* a;
};

int f(int a) {
  if (global != 0) {
	  intLock();
	  true_unsafe = 1;
	  true_unsafe2 = 1;
	  intUnlock();
	  if (global == 0) {
		  true_unsafe2 = 0;
	  }		
  }
  if (((struct A *)23)->a != 0) {
	  intLock();
	  true_unsafe3 = 0;
	  intUnlock();
	  if (((struct A *)23)->a == 0) {		  
	    true_unsafe3 = 0;
	  }
  }
  if (a != 0) {
	  intLock();
	  true_unsafe4 = 1;
	  intUnlock();
  }
} 

int main() {
  global = 0;
	true_unsafe4 = 0;
  ((struct A *)23)->a = 0;
  f(global);
  true_unsafe = 0;
}

int ldv_main() {
	main();
}
