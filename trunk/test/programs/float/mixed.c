int main() {
   int i = 0;
   int j = 123;
   double a = 1.5;
   float b = 2.2;

   double c = a + b; // 3.7
   double d = b - a; // 0.7
   double e = b / a; // 1.4666666666666666666666666666667
   double f = a * b; // 3.3
   double g = j + a; // 124.5
   double h = j + b; // 125.2

   //printf("c: %e d: %e e: %e f: %e g: %e h: %e\r\n", c, d, e, f, g, h);
   if ((3.7 - c) > 0.1) {
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
   if ((123.5 - g) > 0.0) {
      goto Error;
   } else {
      i = 5;
   }
   double x = 125.2 - h;
   if (x > 0.0) {
      goto Error;
   } else {
      i = 6;
   }
   //printf("%s", "Alles passte!\r\n");
   return 0;

   Error: //printf("ERROR %i", i);
   return -1;

}
