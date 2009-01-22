int main(void){
  
  int nonDet;
  unsigned long l;
  int l2;
  int a;
  int s_hit;
  int neg;
  int i;
  int j;  
  int new;

  neg = 0;
  j = -8;
  a = -3;
  i = -a;
  l = -5 + i;
  s_hit = j - 2;
  l2 = neg - a;  
  nonDet = -2 * a;
  new = nonDet * -3;  


  if(j + 8){
   goto ERROR;
  }
  else{}

  if(i - 3){
    goto ERROR;
  } 
  else{
  }
  if(a != -3){
   goto ERROR;
  }
  else {}
  if(neg){
   goto ERROR;
  }
  else{}
  if(l + 2){
   goto ERROR;
  }
  else{}
  if(s_hit != -10){
    goto ERROR;
  }
  else{}

  if(l2 != 3){
    goto ERROR;
  }
  else{}

  if(nonDet != 6){
    goto ERROR;  
  }
  else {}

  if(new - 18){
    goto ERROR;
  }
  else {}
  
  return (0);
  ERROR:
  return (-1);
 
}
