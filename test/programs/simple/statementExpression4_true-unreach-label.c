// inside expression list, value used

int main() {
  int i = (4, ({int c; c = 42;}) );

  if(i != 42) {
    ERROR: // unreachable
      return 1;
  }

  return 0;
}
