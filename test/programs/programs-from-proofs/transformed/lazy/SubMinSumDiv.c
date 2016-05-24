void main()
{
  int s, x, y, z;
  z=0;
  if(x<0)
  {
    if(y<x)
      z=-y;
    else
      z=-x;
    z=z+10;
  }
  else
  {
    if(y>=0)
      s=1;
    else
      s=-y;
    while (x>=y && !(x==0))
    {
      if(y>=0)
        z=z+x;
      else
        z=z+1;
      x=x-s;
    }
  }
}