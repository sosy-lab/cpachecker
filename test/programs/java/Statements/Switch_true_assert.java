
public class Switch_true_assert {

public static void main(String[] args) {

  int n1;

  n1 = 10;

  switch(n1){
    case 1: 
      assert (false);
      break;
    case 5:
      assert (false);
      break;
    case 10:
      // this branch happens
      assert(true); // always true
      break;
    default:
      assert (false);
      n1 = 1;
  }

  n1 = 6;

}
}
