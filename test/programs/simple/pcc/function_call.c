int compute_square(int y)
{
   return y+3;
}

int main()
{
  int x = 2;
  x= compute_square(x)+ 2; 
  x = compute_square(x) + 2;
  if(x!=12)
  {
     goto ERROR;
  }
  return 0;

ERROR: return -1;
}
