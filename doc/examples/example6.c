int main() {
  int i = 0;
  int a = 0;
	int j = 0;
	int b = 0;

  while (1) {
    if (i == 20) {
       goto LOOPEND;
    } else {
       i++;
       a++;
    }

    if (i != a) {
      goto ERROR;
    }
  }

while(1){
	if (j == 20){
		goto LOOPEND;
			
} else {
	j++;
	b++;
}

if (j != b){
	goto ERROR;
}
}

  LOOPEND:

  if (a != 20) {
     goto ERROR;
  }
	else if (j != 20){
	goto ERROR;
	}

  return (0);
  ERROR:
  return (-1);
}

