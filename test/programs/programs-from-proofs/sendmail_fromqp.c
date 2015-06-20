extern int __VERIFIER_nondet_int();
int flag=0;

int main (void)
{
  // XXX infile originally at most MAXLINE long per call to mime_fromqp

  int BASE_SZ = 2;
  int outfile[BASE_SZ]; // originally MAXLINE
  int c1, c2;
  int nchar = 0;
  int out = 0; // index into outfile

  while (__VERIFIER_nondet_int())
  {
    if (__VERIFIER_nondet_int())
    {
      // malformed: early EOS
      if (__VERIFIER_nondet_int())
	break; 

      // =\n: continuation; signal to caller it's ok to pass in more infile
      // OK: reset out before taking more input
      if (__VERIFIER_nondet_int())
      {
	out = 0;
	nchar = 0;
	continue;
      }
      else
      {
	// convert, e.g., "=5c" to int

	// malformed: early EOF
	if (__VERIFIER_nondet_int())
	  break;

	nchar=nchar+1;
	if (nchar > BASE_SZ)
	  break;

	/* OK */
	flag=out;
	outfile[out] = c1;
	out=out+1;
      }
    }
    else
    {
      // regular character, copy verbatim

      nchar=nchar+1;
      if (nchar > BASE_SZ)
	break;

      /* OK */
      flag=out;
      outfile[out] = c1;
      out=out+1;

      if (__VERIFIER_nondet_int())
	break;
    }
  }

  /* OK */
  flag=out;
  outfile[out] = 1;
  out=out+1;
  return 0;
}
