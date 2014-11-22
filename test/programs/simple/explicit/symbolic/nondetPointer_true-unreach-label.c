
int main() {
  int var = 100;
  int * pointer1 = &var;
  int * pointer2 = pointer1;
  
  if (pointer1 == pointer2) {
    return 0;
  } else {
ERROR:
    return -1;
  }
}
