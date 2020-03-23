int main() {
	int array[] = {0};
	int den = 4;
	int num = 2;
	int index = den/num;

	if(index != 0) goto EXIT;
		return array[index];

EXIT: return 0;
ERROR: return 1;
}

