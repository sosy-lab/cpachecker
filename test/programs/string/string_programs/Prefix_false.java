package ownPrograms;

public class Prefix_false {

  public static void main(String[] args) {
    String s1 = "batman";
    String prefix = "tma";
    assert s1.startsWith(prefix);

    String s2 = "superman";
    String s3 = s1 + s2;
    assert s3.startsWith(prefix);
  }

}
