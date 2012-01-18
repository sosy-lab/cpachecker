int main()
{
  int x = 0, y = 0;
  int m;
  int n = 2 * m;

  while (x < n) {
    if (x < m) {
      x ++;
      y ++;
    } else {
      x ++;
      y --;
    }
  }
  if(y != 0) {
    ERROR: goto ERROR;
  }
  return 0;
} 
