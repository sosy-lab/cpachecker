extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();

  int diff = a - b; // diff = ?
  int returnValue;

  if (diff > 0) { // diff = ?
    returnValue = 1; // returnValue = 1
  }

  if (diff < 0) { // diff = ?
    returnValue = -1; // returnValue = -1
  }

  if (diff == 0) { // diff = ?
    returnValue = 0; // returnValue = 0
  }

  if (returnValue < -1 || returnValue > 1) { // returnValue = ?
ERROR:
    return -10;

  } else {
    return returnValue;
  }
}
