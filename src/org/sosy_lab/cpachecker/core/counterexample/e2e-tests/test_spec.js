describe('Report.js test', function() {
  it('Title test', function() {
    var dirname =  __dirname + '/Counterexample.1.html';
    dirname = dirname.replace(/\\/g, "/");
    browser.waitForAngularEnabled(false);
    browser.get(dirname);
    expect(browser.getTitle()).toEqual('CPAchecker Report');
  });
});