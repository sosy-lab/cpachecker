int main(int argc,char* argv[])
{
	int a = 0;
	int i = 0;
	int c = 0;
	char* string = "Test";

	while(i<5) {
		i++;
		a++;
	}

	c = i;
	string++;

	while(i<10) {
		i++;
		a++;
	}

	c = i;

	string++;
	if(a<10) {
		ERROR:
		goto ERROR;
	}

	return 0;
}

