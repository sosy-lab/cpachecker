extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  int b = a;

  a = a + 10;
  b = b + 10;

  if (a != b) {
    goto ERROR;
  }
  
  b = b - 10;

  if (b != a - 10) {
    goto ERROR;
  } else {
    a = a - 10;
  }

  b = (b * 10 / 2) % 7;
  a *= 10;
  a = (a / 2) % 7;

  if (a != b) {
    goto ERROR;
  }

/*  int temp = b;
  b = b >> a;
  a = a >> temp;

  if (a != b) {
    goto ERROR;
  } */

  a = - a;
  b = - b;

  a = ~ b;
  b = ~ a;

  if (~ a == b) {
    return 0;
  } else {
ERROR:
    return -1;
  }
}
