extern int __VERIFIER_nondet_int();
void reach_error();

int t() {return __VERIFIER_nondet_int();}

int main() {


  int x = 0;
  int water = 0;
  int constant = 1;

  for (int i = 0; i < 4; i++) {
    if (constant == 0 && t()) water ++;
    if (water == 3 && t()) constant = 0;
    if (x == 1 && t()) water = 3;
    x++;
  }

  if (water == 4) reach_error();

}
