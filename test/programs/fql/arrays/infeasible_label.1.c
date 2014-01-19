int __CPROVER_assume(int condition);

int main(int argc, char* argv[]) 
{
  int tmp1;
  int tmp2;
  int tmp3;
  int tmp4;
  int __BLAST_NONDET;
  int a[3];

  a[0] = __BLAST_NONDET;
  a[1] = __BLAST_NONDET;
  a[2] = __BLAST_NONDET;

  tmp1 = a[0];
  tmp2 = a[1];

  __CPROVER_assume(tmp1 == 10);
  __CPROVER_assume(tmp2 == 0);
  
  tmp3 = a[0];
  tmp4 = a[1];

  if (tmp3 < tmp4)
  {
L:
    return (0);
  }
  else
  {
    return (1);
  }
}

