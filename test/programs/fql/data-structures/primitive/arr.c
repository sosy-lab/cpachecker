

int main(){


  int d[5][7];
  int k[5];
  
  d[1][1]=23;
  d[1][1]= d[1][1]+4;
  int *test;
  test = &d[4][6];
  *test =5;
  *test = *test+3;
  
}
