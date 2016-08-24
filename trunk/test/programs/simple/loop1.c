int i;
int a;

int main() {
  i = 0;
  a = 0;

  while(1) {
    if(i== 20){
       goto LOOPEND;
    }
    else{
       i++;
       a++;
    }
  }

  LOOPEND:

  if (a != 20) {
     goto ERROR;
  }
  else {}
  
  return (0);
  ERROR:
  return (-1);
}

