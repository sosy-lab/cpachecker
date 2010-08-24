void __CPROVER_assume(int);

int foo(int x) 
{
  int tmp;

  tmp = x > 3;

  __CPROVER_assume(tmp);

  while (1) 
  {
    if (x > 0) 
    {

    } 
    else 
    {
      break;
    }

L: ;
    x = x - 1;
  }

  return (0);
}

