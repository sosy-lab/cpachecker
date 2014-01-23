int __VERIFIER_nondet_int();

#define INPUT __VERIFIER_nondet_int

int main() 
{ 
  int x, y, z;
  int result;

  int flag, flag2;

  flag = INPUT();

  if (flag) {
    flag2 = 0;
    x = INPUT();
    y = INPUT();
  }
  else {
    flag2 = 1;
    x = INPUT();
    y = 3;
  }

  z = INPUT();

  if (flag2) {
    result = (x + z > 10);
  }
  else {
    result = (x + z <= 10);
  }

  if (result) {
    result = 10;
  }

  return result;
}

