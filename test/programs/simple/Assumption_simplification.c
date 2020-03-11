/* The test should check the simplification of assumptions
 * There was a bug, when if(! a != 0) was converted to if (a != 0)
 */ 

int main() {
  int a = 0;
  if (! a != 0) {
    ERROR:
    0;
  }
}
