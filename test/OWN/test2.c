int main(int argc,char* argv[])
{
	int a = 0;
	int i = 0;
	int c = 0;

	while(i<10) {
		i++;
		a++;
	}

	c = i;
	if(c==5) {
		c = 23;
		a++;
		a--;
	}

	if(a<10) {
		ERROR:
		goto ERROR;
	}

	return 0;
}
