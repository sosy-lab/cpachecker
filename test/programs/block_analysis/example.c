extern int x(int y);
void reach_error(){}
void main() {
  
  int a = x(1);
  int b = x(2);
  int c = x(1);

  if (a != c) reach_error();

}
