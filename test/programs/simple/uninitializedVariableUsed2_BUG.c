void __fail(void) {
   ERROR: goto ERROR;
}

int main(void) {
   int a,x;
   if(a>0) {
       x=2;
   } else {
       x=3;
   }
   if(x==2) __fail();
   return a;
}
