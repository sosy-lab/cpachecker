extern int __VERIFIER_nondet_int();

int flag1=0;
int flag2=0;

void main()
{
  int i=0;
  int j=0;
  int bufsz=6;
  int buffer[bufsz];//6
  int len = bufsz+5;
  int msg[len];// 11

  int limit = bufsz - 4;

  while(i < len) {
    while(i < len && j < limit){
      if (i + 1 < len && __VERIFIER_nondet_int()) {
		flag1=j;
		flag2=i;
        buffer[j] = msg[i];
        j=j+1;
        i=i+1;
        flag1=j;
        flag2=i;
        buffer[j] = msg[i];
        j=j+1;
        i=i+1;
        /* OK */
        flag1=j;
        buffer[j] = '.';
        j=j+1;
      } else {
		flag1=j;
        flag2=i;
        buffer[j] = msg[i];
        j=j+1;
        i=i+1;
      }
    }
  }
}
