# 1 "typedef.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 1 "typedef.c"
typedef int *PNT;
typedef int ARR[5];
typedef PNT ARR2[5];
typedef ARR ARR3[3][10];

struct stest{
  int x;
  int y;

}asdfsd;

typedef struct stest var[5];
typedef var field[5];
int main(){
  int x;
  PNT a;
  ARR k;


  a = &x;
  *a=3+4;
  x = x*7;
  k[1]=x+4;
  ARR2 data;
  data[4]=a;
  *data[4]=x+7;
  ARR3 overkill;
  overkill[2][9][4]=55;
  overkill[2][9][3]=3;
  var sdata;
  sdata[0].x = 44;
  field test;
  test[0][0].x=44;
  return 0;
}
