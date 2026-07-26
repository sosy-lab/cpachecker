int main () {

  int x = __VERIFER_nondet_int();
  for (int i = 0; i < 1050; i++) {
     x++;
  }
  int j = 0;
  for (; j < 5; j++) {}
  if (j != 5) reach_error();

}
