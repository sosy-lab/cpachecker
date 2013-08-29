
int main(void){
  
  int a = 0;
  {
    __label__ testlabel;
    testlabel:

    if(a < 1) {
      a++;
      goto testlabel;
    }
  }

  {
   __label__ testlabel;

   int x = 2;
   if(x == 2) {
     x = 3;
     goto testlabel;
   }

   testlabel:

    if(a < 1) {
      a++;
      goto testlabel;
    }
  }

  {
   __label__ testlabel;
   testlabel:

    if(a < 1) {
      a++;
      goto testlabel;
    }
  }


  return 0;
}

