
void abort() {

}

int f(int x, int y) {
  int z;
  int tmp1;
  int tmp2;
  int tmp3;

  z = y;

  tmp1 = (x == z);

  if (tmp1)
  {
	tmp2 = (x + 10);
	tmp3 = (y == tmp2);

    if (tmp3)
    {
      abort();
    }
  }

  return (0);
}

