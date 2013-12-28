<#include "_header.ftl">

<form action="/jobs" method="POST" enctype="multipart/form-data">
	<fieldset>
		<legend>${msg.settings}</legend>

    <p>
      <label for="specification">${msg.specification}</label><br>
      <select name="specification" id="specification">
        <#list specifications as specification>
          <#assign name = specification?substring(specification?last_index_of("/")+1)>
          <option value="${name}">${name}</option>
        </#list>
      </select>
    </p>

    <p>
      <label for="configuration">${msg.configuration}</label><br>
      <select name="configuration" id="configuration">
        <#list configurations as configuration>
          <#assign name = configuration?substring(configuration?last_index_of("/")+1)>
          <option value="${name}">${name}</option>
        </#list>
      </select>
    </p>

    <p>
      <label for="programFile">${msg.programFile}</label><br>
      <input type="file" name="programFile" id="programFile">
    </p>

    <p>
      <label for="programText">${msg.programText}</label><br>
      <textarea name="programText" id="programText" cols="30" rows="10"></textarea>
    </p>
	</fieldset>
	
	<fieldset>
		<legend>${msg.options}</legend>

    <p>
      <label for="foo">Foo</label>
      <select name="options[]" id="foo">
        <option value="foo=bar">bar</option>
        <option value="foo=baz">baz</option>
      </select>
    </p>
    <p>
      <label for="foobar">Foobar</label>
      <select name="options[]" id="foobar">
        <option value="foobar=barbaz">barbaz</option>
        <option value="foobar=foo">foo</option>
      </select>
    </p>

    <ul>
    <#list defaultOptions?keys as option>
      <li>${option}: ${defaultOptions[option]}</li>
    </#list>
    </ul>
	</fieldset>

  <input type="submit" value="${msg.submitJob}">
</form>

<#include "_footer.ftl">