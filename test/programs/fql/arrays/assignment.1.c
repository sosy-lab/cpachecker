void init(int a[]) 
{
  a[0] = 0;
  a[1] = 10;
  a[2] = 20;

  return;
}

int main(int argc, char * argv[]) 
{
  int A[3];
  int tmp1;
  int tmp2;
  int tmp3;

  init(A);

  tmp1 = A[0];
  tmp2 = A[1];

  if (tmp1 > tmp2) 
  {
L:
    tmp3 = 1;
  }
  else
  {
    tmp3 = 0;
  }

  return (tmp3);
}

