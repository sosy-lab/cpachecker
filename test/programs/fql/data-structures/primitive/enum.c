


enum test{
  x=1,
  y=2
};

typedef enum test x1;


int main(){
  int v;
  enum test data;
  enum test *pnt;
  data = y;

  v=data;
  v++;
  pnt = &data;
  *pnt =x;
  v = v+ data;
  x1 data2;
  data2= data;
  v+= data2;
}
