int main(){
	int x = 0;
	int y = x;
	if (y == 0)
		goto ERROR;
EXIT:  return 0;
ERROR: return 1;
}
