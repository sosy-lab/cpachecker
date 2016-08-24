int main(){
int a=0;
int i=0;

for(;i<10;i++){
	a++;
}

if(a!=i ||  i!=10){
	goto ERROR;
}

return 0;

ERROR: 
return -1;}