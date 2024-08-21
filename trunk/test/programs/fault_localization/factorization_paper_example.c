int __VERIFIER_nondet_int();


int main(){
 int num = __VERIFIER_nondet_int();

 if (num < 1) return 0;

 for (int i = 2; i <= num; i++) {
   if (num % i == 0) {
     num /=(i + 1);
     i--;
   }
 }

 if(num != 1) {
   ERROR: return 1;
 }
}
