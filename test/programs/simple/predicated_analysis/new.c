void main()
{
  int y;
  int x=5;
  
  if (y != x) {
     goto ERROR;
  }
y=6;
y=7;
  return (0);
  ERROR:
  return (-1);
}
