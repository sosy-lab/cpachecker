/* This test should check, how the tool handle loop variables
 */
int global;

int f() 
{ 
  int i = 0;
  while (1) {
    if (i < 10) {
        
    } else {
      break;
    }
    i = i + 1;
  } 
  global = 1;
  return (0);
}

int ldv_main() {
	f();
}

