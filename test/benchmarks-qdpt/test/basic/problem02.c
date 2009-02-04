void foo(int param) {
  int val;

  val = param;

  if (val == 10) {
    param++;
  }
  else {
    param--;
  }
}

int main(void) {
  int s1;

  s1 = 10;

  foo(s1);

  return (0);
}

