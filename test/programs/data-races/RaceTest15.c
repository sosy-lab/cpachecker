/* This test should check, how the tool handle repeated usages (there are several ones in line 12 and 13)
 * There was a difficult bug, when one usage is replaced by another
 */
int unsafe;

int f(int t ) 
{ 
  int oldflags ;
  int s ;
  if (t) {
      unsafe = unsafe & -1051265;
      unsafe = unsafe | 16777216;
      s = splbio();
      //It is important to have if here to get several states with different path formulas.
      //The have the same usages in container and try to add them. 
      //We should handle this situation
      if (oldflags) {
        func();
      }
      splx(s);
  }
  return (0);
}

int ldv_main() {
	f(0);
}

