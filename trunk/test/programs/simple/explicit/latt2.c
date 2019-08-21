void error()
{
	ERROR: goto ERROR;
}

int nondet();
int call();
int some();
int useless();
int functions();
int to();
int make();
int other();
int branches();
int proceed();

int main()
{
	int a,b,z;
	if (nondet()){
		a=10; b=1;
	}else{
		if (nondet()){
			a=1; b=10;
		}else{
			a=1; b=1;
			call();      //due to DFS traversal, this branch may be at z=10 before the other!
			some();
			useless();
			functions();
			to();
			make();
			other();
			branches();
			proceed();
		}
	}
  z = 10; // coverage error happens here
	if (a==1 && b==1) error(); // and leads to missing this error!
	return 0;
}
