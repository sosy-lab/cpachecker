// This is SAFE.
int main() {
   int i = 0;
   int j = 123;
   int a = 1;
   int b = 2;

   int c = a + b; // 3
   int d = b - a; // 1
   int e = b / a; // 2
   int f = a * b; // 2
   int g = j + a; // 124
   int h = j + b; // 125

   //printf("c: %i d: %i e: %i f: %i g: %i h: %i\r\n", c, d, e, f, g, h);
   if ((3 - c) > 0) {
      goto Error;
   } else {
      i = 1;
   }
   if ((1 - d) > 0) {
      goto Error;
   } else {
      i = 2;
   }
   if ((2 - e) > 0) {
      goto Error;
   } else {
      i = 3;
   }
   if ((2 - f) > 0) {
      goto Error;
   } else {
      i = 4;
   }
   if ((124 - g) > 0) {
      goto Error;
   } else {
      i = 5;
   }
   if ((125 - h) > 0) {
      goto Error;
   } else {
      i = 6;
   }
   //printf("%s", "Alles passte!\r\n");
   return 0;

   Error: //printf("ERROR %i", i);
   return -1;

}
