int i;
int a;

void main() {
  i = 0;
  a = 0;

  while(1) {
    if(i== 5){
       break;
    }
    else{
       i++;
       a++;
    }
  }

  LOOPEND:

  if (a != 5) {
     goto ERROR;
  }
  else {}
  
  return;
  ERROR:
  return;
}

