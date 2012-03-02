
extern int rand(int);

int main(void) {

  int a;
  int b;
  int c;
  int d;

  d = 1;

	if(a)
;//		b = 1;
	else
		c = 2;

  if(!d)
    goto ERROR;
  else
     goto EXIT;

  ERROR:
    goto ERROR;

  EXIT:
    ;  
}

