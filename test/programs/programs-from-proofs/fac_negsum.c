void main() 
{
  int flag;
  int z;
  int y;
  int x;
  x=0;
  if(flag == 1) 
  {
    x=1;
  }
  while (y>0)
  {
    if(flag ==1)
    {
      x=x*y;
    }
    else
    {
      x=x-y;
    }
    y=y-1;
  }
}
