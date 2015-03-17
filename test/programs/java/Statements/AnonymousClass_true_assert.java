
public class AnonymousClass_true_assert {

  public static void main(String[] args) {
    SimpleInterface f = new SimpleClass(5) {
      @Override
      public int getIdentity(int x) {
        return x;
      }
    };
    SimpleInterface sameAsF = new SimpleClass(5) {
      @Override
      public int getIdentity(int x) {
        return x;
      }
    };

    SimpleInterface2 other = new SimpleInterface2() {

      private int n = 2;

      @Override
      public int addOne(int x) {
        return minusOne(x) + n;
      }

      private int minusOne(int x) {
        return x - 1;
      }

    };

    assert 5 == f.getIdentity(5);
    assert 4 == other.addOne(3);
    assert ((SimpleClass) sameAsF).z == ((SimpleClass) f).z;
  }

  private abstract static class SimpleClass implements SimpleInterface {
    private int z;

    protected SimpleClass(int y) {
      z = y - 1;
    }

    @Override
    public int addOne(int x) {
      return x + 1;
    } 

  }

  private interface SimpleInterface extends SimpleInterface2 {
    int getIdentity(int x);
  }

  private interface SimpleInterface2 {
    int addOne(int x);
  }
}
