#include<assert.h>
extern _Bool __VERIFIER_nondet_bool();

int main() {
   int i = 0;
   while(__VERIFIER_nondet_bool()){
      if (i == 4) {
         i = 0;
      }
      i++;
   }
   assert(i >= 0 && i < 5);
}
