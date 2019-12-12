int l[]={0,1};
int main(){
     int*p=l;
     if(*p++ == 0) return 0;
     {ERROR:goto ERROR;}
     return 0;
}
