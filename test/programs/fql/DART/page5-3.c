
void abort() {

}

void foobar(int x, int y){
  int tmp1;
  int tmp2;
  int tmp3;
  int tmp4;
  int tmp5;
  int tmp6;
  int tmp7;

  tmp1 = x * x;
  tmp2 = tmp1 * x;
  tmp3 = (tmp2 > 0);

  if (tmp3)
  {
	tmp4 = (x > 0);

    if (tmp4)
    {
      tmp5 = (y == 10);

      if (tmp5)
      {
        abort();
      }
    }
  } 
  else 
  {
	tmp6 = (x > 0);

    if (tmp6)
    {
      tmp7 = (y == 20);

      if (tmp7)
      {
        abort();
      }
    }
  }
}

