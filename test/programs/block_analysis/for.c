void reach_error(){}

int main() {

  int i = 0;
  int j = 0;
  for (; j < 10; i++) j++;

  if (i != j) reach_error();

}
