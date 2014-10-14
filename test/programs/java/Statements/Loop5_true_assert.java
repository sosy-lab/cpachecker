
public class Loop5_true_assert {

  public static void main(String[] args) {
    int n1;

    n1 = 0;

    do {
      n1 = n1 + 1;
      continue;

      n1 = 40;

    } while (n1 > 0 && n1 < 10);

    assert (n1 == 10); // n1 == 10 = true
  }
}
