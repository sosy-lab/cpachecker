void __CPROVER_assume(int condition);

void foo() 
{
  int x;
  int y;
  int z;
  int cond;
  int __BLAST_NONDET;

  x = __BLAST_NONDET;

  if (x)
  {
    y = __BLAST_NONDET;
    cond = (y == 10);
    __CPROVER_assume(cond);
  }
  else 
  {
    y = 0;
  }

  if (!x)
  {
    z = __BLAST_NONDET;
    if (z == 13)
    {
L:;
      y = z;
    }
  }
}

