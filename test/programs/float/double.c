int main() {
   int i = 0;
   double a = 1.5;
   double b = 2.2;

   double c = a + b; // 3.7
   double d = b - a; // 0.7
   double e = b / a; // 1.4666666666666666666666666666667
   double f = a * b; // 3.3

   //printf("c: %e d: %e e: %e f: %e\r\n", c, d, e, f);
   if (c != 3.7) {
      goto Error;
   } else {
      i = 1;
   }
   // With doubles, 0.0 works, with floats not
   if ((0.7 - d) > 0.0) {
      goto Error;
   } else {
      i = 2;
   }
   if ((1.47 - e) > 0.1) {
      goto Error;
   } else {
      i = 3;
   }
   if ((3.3 - f) > 0.0) {
      goto Error;
   } else {
      i = 4;
   }
   //printf("%s", "Alles passte!\r\n");
   return 0;

   Error: //printf("ERROR %i", i);
   return -1;

}
