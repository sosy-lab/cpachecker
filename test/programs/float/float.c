int main() {
   int i = 0;
   float a = 1.5;
   float b = 2.2;

   float c = a + b; // 3.7
   float d = b - a; // 0.7
   float e = b / a; // 1.4666666666666666666666666666667
   float f = a * b; // 3.3

   //printf("c: %e d: %e e: %e f: %e\r\n", c, d, e, f);
   if (c != 3.7f) {
      goto Error;
   } else {
      i = 1;
   }
   if ((0.7f - d) > 0.1f) {
      goto Error;
   } else {
      i = 2;
   }
   if ((1.47f - e) > 0.1f) {
      goto Error;
   } else {
      i = 3;
   }
   if ((3.3f - f) > 0.1f) {
      goto Error;
   } else {
      i = 4;
   }
   //printf("%s", "Alles passte!\r\n");
   return 0;

   Error: //printf("ERROR %i", i);
   return -1;

}
