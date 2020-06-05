int main() {

	int val = 50;
	int i = 1;
	int v = 0;
	int res = 0;
	while(v < val) {
		v = v + 2*i + 1;
		i++;
	}
	res = i;
	if (res*res <= val && (res+1)*(res+1)>val)
		goto EXIT;
	else
		goto ERROR;

EXIT: return 0;
ERROR: return 1;
}
