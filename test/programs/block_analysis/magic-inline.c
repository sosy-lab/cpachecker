void reach_error(){}

int main() {

   int count = 0;

   for (int i = 0; i < 6; i++) {
   
     int m = 0;
     if (i >= 0 && i < 5) m = 1;
     else m = 0;
     if (m != 1) count++; 
   
   }
   
   if (count == 1) reach_error();

}
