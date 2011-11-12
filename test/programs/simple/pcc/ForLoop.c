int main(){
int a=0;

for(int i=0;i<10;i++){
	a++;
}

if(a!=i | i!=10){
	goto ERROR;
}

return 0;

ERROR: 
return -1;}