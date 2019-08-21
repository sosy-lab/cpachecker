int main() {
	int a = 6;
	int b = 6;
	int c = 6;
	int d = 6;
	int e = 6;


	for(a = 0; a < 6; ++a) {
		for(b = 0; b < 6; ++b) {
			for(c = 0; c < 6; ++c) {
				for(d = 0; d < 6; ++d) {
					for(e = 0; e < 6; ++e) {
						
					}
				}
			}
		}
	}	
	if(!(a == 6 && b == 6 && c == 6 && d == 6 && e == 6)) {
		ERROR: goto ERROR;
	}
	return 1;
}
