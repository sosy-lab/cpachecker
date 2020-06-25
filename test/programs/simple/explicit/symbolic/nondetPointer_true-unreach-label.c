
int main() {
  int var = 100;
  int * p1 = &var;
  int * p2 = p1;
  
  if (p1 != p2) {
    
  }

  if (p1 == p2) {
    return 0;
  } else {
ERROR:
    return -1;
  }
}
