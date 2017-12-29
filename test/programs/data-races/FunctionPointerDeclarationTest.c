/* The test should check processing of function pointers with different declarations */

struct F {
  int (*a)();
  int (*b)(void);
  int (*c)(int d);
} *var;

int func(int a) {
  return 0;
}

int ldv_main() {
    int t;
    
  intLock();
  var->a = &func;
  var->b = &func;
  var->c = &func;
  intUnlock();
  
  var->a(t);
  var->b(t);
  var->c(t);
}

