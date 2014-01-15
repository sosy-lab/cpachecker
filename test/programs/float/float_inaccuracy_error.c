int main() {
   int j = 123;
   float b = 2.2;
   float h = j + b; // 125.2
   double c = 125.2;
   
   if (h - c != 0) {
      goto Error;
   }
   
   return 0;

   Error:
   return -1;
}
