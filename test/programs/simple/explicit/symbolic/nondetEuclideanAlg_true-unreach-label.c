extern __VERIFIER_nondet_int();
int main() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();
  
  if (a < 0) {
    a++;
    if (a < 0) {
      a = -a;
    }
  }

  if (b < 0) {
    b++;
    if (b < 0) {
      b = -b;
    }
  }
  
  if (a == 0) {
    return a;

  } else {
    while (b != 0) {
      if (b < 0) {
        goto ERROR;
      }
      
      if (a > b) {
        a = a - b;

      } else {
        if (a < 0) {
          goto ERROR;
        }

        b = b - a;
      }
    }
    return a;
  }

ERROR:
  return -1;
}
