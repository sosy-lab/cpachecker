int main() {
	int a = 6;
	int b = 6;
	int c = 6;
	int d = 8;

	if(a == 6) {
		a = 7;
	} else {
		a = 66;
	}

	if(c == 5) {
		c = 6;
	} else {
		goto ERROR;	
	}

	if (a == 99) {
		goto END;
	}



	if (d == 13) {
		d = 12; 
	}else {		
		ERROR: return 1;
	}

	END: return 0; 
}
