void reach_error(){}
extern int __VERIFIER_nondet_int();

int magic(int i) {
   
   if (i == 0) return 1;
   else return 0;

}

int main() {

   int count = 0; // counti();
   int i = 0; //counti();
   int in = __VERIFIER_nondet_int();
   if (in < 4) return 0;
   for (; i < in; i++) {
   
     int m = 0;
     if (i >= 0 && i < in) m = magic(0);
     else m = magic(1);
     if (m != 1) count++; 
   
   }
   
   if (count == 0) reach_error();

}

