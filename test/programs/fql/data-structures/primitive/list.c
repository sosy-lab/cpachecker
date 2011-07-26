
struct list{
  int x;
  struct list *next;
};


int main(){
  int x;
  x=0;
  struct list item1;
  struct list item2;
  struct list item3;
  item1.next =  &item2;
  item2.next = &item3;
  item3.next = 0;

  struct list *it;
  it = &item1;
  while(it->next !=0){
    it->x = x+1;
    it = it->next;
    x++;
  }

  return 0;


}



