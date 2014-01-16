int main() {
   int i = 0;
   int j = 123;
   float b = 2.2;
   double h = j + b; // 125.2
   
   double x = 125.2 - h;
   if (x > 0.0) {
      goto Error;
   }
   return 0;

   return -1;

}
