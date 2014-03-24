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

	if(a<20) {
		ERROR:
		goto ERROR;
	}

	return 0;
}

