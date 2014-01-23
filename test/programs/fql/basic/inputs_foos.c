int __VERIFIER_nondet_int();

#define INPUT __VERIFIER_nondet_int

int main() 
{ 
  int x, y;
  int result;

  x = INPUT();
  y = INPUT();

  if (x > y) {
    result = 0;
  }
  else {
    result = 1;
  }

  return result;
}

