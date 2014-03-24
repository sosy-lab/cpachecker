int main(int argc,char* argv[])
{
	int a = 0;
	int i = 0;
	int c = 0;

	for(int k=0;k<5;k++) {
		c++;
	}

	while(i<10) {
		i++;
		a++;
	}

	c = i;

	if(a<10) {
		ERROR:
		goto ERROR;
	}

	return 0;
}
