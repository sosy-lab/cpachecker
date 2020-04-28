extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern void __VERIFIER_assume(int cond) { if(!cond) while(1){} }
void __VERIFIER_assert(int cond) { if(!(cond)) { ERROR: __VERIFIER_error(); } }
extern int __VERIFIER_nondet_int();

int
main() {
   int reg = 0;
   while (1) {
      reg = !reg;
      if (reg == 2) {
         __VERIFIER_assert(0);
         return 1;
      }
   }
   return 0;
}
