extern int __VERIFIER_nondet_int();

int main ()
{
	
  int pattern[8];
  int pathend[3];
  int pathlim =2;
  int i;
  int anymeta = 0;
  int tmp;
  int pos;

  while(1) {

    /* Copies a single string from pattern into pathend, checking for 
     * the presence of meta-characters.
     */
    i = 0;
    while (__VERIFIER_nondet_int()) {
	  pos=0;
      anymeta = pattern[i]; 
      if (pathend + i >= pathlim)
        return 1;
      pos=0;
      tmp = pattern[i];
      /* OK */
      pos=1;
      pathend[i] = tmp;
      i=i+1;
    }

    if (__VERIFIER_nondet_int())
      return 0;
  }

  /* NOT REACHED */
}
