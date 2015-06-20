int main(){
  int a;
  int b;
  int c;
  int isoscles = 0;
  int scalene=0;
  int triangle=0;
  int equilateral=0;
  int s;
  
  if (a > 0 && b > 0 && c > 0 && a < b +c){
    triangle = 1;
  } else {
    triangle=-1;
  }

  if (a >= b){
    if ( b >= a){
      isoscles = 1;
    }
  } 
  
  if (b >= c){
    if (c >= b){
      isoscles =1 ;
    }
  }

  if (a >= b){
    if (b >= c){
      if (c >= a){
	equilateral =1;
      }
    }
  }
  
  if (a + b > c){
    triangle=1;
  
    if (isoscles == 0 || equilateral == 0){
      scalene=1;
    }
  }
  
  s=0;
  
  return equilateral+isoscles + triangle+scalene;
}
