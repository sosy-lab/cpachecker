int main(){
	
	int a = 1;
	int b = 2;
	if(a < b)
		if(a < b)
			goto ERROR;


EXIT: return 0;
ERROR: return 1;
}
