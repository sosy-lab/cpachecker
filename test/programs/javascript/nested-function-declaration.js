function foobar() {
  function bar() {
    var nested = 1
  }

  bar();
}

foobar();
