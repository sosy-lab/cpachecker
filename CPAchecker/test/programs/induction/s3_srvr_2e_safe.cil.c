extern void __assert_fail();

int main(void) {

  int s__state = 1;
  int blastFlag = 1;
  
  while (1) {
  
    if (s__state == 1) {
    
    } else if (s__state == 2) {
    
      s__state = 3;
      
    } else if (s__state == 3) {
    
      if (blastFlag == 2) {
        blastFlag = 3;
      }
      s__state = 4;
      
    } else {
    
      if (blastFlag == 4) {
        __assert_fail();
        return -1;
      }
      return 0;
      
    }
  }
}
