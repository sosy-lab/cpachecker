package pack1.pack2;

public class Object2 {

  static int idGenerator = 0;
  int id = 0;

  public Object2(){
    id = idGenerator;
    idGenerator++;
  }

  public Object2(int id , int n1 , int n3 , int name){
    this.id = id;
    idGenerator = id;

    id = idGenerator;
    idGenerator++;
  }

  public int getId(){
    return id;
  }
}
