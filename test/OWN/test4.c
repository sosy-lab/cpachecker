int main(int argc,char* argv[])
{
	int a = 0;
	int i = 0;
	int c = 0;

	while(i<5) {
		i++;
		c = 0;
		while(c<3) {
			c++;
		}
		a++;
	}

	c = i;

	if(a<5) {
		ERROR:
		goto ERROR;
	}

	return 0;
}
