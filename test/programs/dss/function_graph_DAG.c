void h(){

}

void g(){
    h();
}

void f(){

    int y;

    if (y){
        return;
    } 

    g();


}

int main(){

    int x;
    f();
    if(x){
        f();
    } else {
        g();
    }

    return 0;

}