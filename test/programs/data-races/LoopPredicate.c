/* This test loops at global variable. The other need to avoid the origin loop handling*/

int global, other;

int f(int a) {
  int b = 0;
  if (a == 0) {
    b++;
  } else {
    global++;
  }
  return b;
}

int h(int a){
    g(a);
    
    if (a == 0) {
      other++;
    }
}

int g(int a) {
  int b = 0;
  if (a == 0) {
    b++;
  } else {
    other++;
  }
  return b;
}

int ldv_main() {
  int t;
  int d;
  
  f(t);
  if (t == 0) {
    global++;
  } 
  h(d);
}
