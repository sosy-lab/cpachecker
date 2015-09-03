int main(void) {

  int cards[] = {[10 ... 15] = 1, 2, [1 ... 5] = 3, 4};
  if (sizeof(cards) != 17*sizeof(int)) {
    ERROR: return 1;
  }
  return 0;
}
