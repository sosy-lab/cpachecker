int main(void) {
  int x;
  int y;
  int z;
  
  if (z) {
    y++;
  }
  else {
    x = 10;
  }

  if (y) {
    goto END;
  }
  else {
    z++;
  }

  if (x < 10) {
    x = 3; 
  }
  else {
    x = 4;
  }

END:

  return (0);
}

