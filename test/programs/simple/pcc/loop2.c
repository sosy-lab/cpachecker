
int a;
int j;

int main() {
  int i = 0;
  a = 0;
  j = 0;

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

while(1) {
    if(j== 20){
       goto LOOPEND2;
    }
    else{
       j++;
       a++;
    }
  }
LOOPEND2:
if(a!=40){
goto ERROR;
}
else{}
  
  return (0);
  ERROR:
  return (-1);
}

