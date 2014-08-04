<#include "_header.ftl">

<div class="container">

<div class="row">

  <form action="/tasks" method="POST" enctype="multipart/form-data">
    <div class="col-md-6">
      <div class="panel panel-default">
        <div class="panel-heading">
          <div class="panel-title">${msg["settings"]}</div>
        </div>
        <div class="panel-body">

          <#if errors??>
          <div class="alert alert-danger">
            <ul class="list-unstyled">
            <#list errors as error>
              <li>${msg[error]}</li>
            </#list>
            </ul>
          </div>
          </#if>

          <#if errors?? && errors?seq_contains("task.specOrConf.IsBlank")>
          <div class="form-group has-error">
          <#else>
          <div class="form-group">
          </#if>
            <label for="specification" class="control-label">${msg["task.spec"]}</label>
            <select name="specification" id="specification" class="form-control">
              <option value="">${msg["noSpecification"]}</option>
              <option value="" disabled>-------------------</option>
              <#list specifications?sort as specification>
              	<#assign name = specification?substring(0, specification?last_index_of("."))>
                <#if specification == "default.spc">
                  <#assign selected = "selected">
                <#else>
                  <#assign selected = "">
                </#if>
                <option value="${specification}" ${selected}>${name}</option>
              </#list>
            </select>
          </div>
          <#if errors?? && errors?seq_contains("task.specOrConf.IsBlank")>
          <div class="form-group has-error">
          <#else>
          <div class="form-group">
          </#if>
            <label for="configuration" class="control-label">${msg["task.conf"]}</label>
            <select name="configuration" id="configuration" class="form-control">
              <option value="">${msg["noConfiguration"]}</option>
              <option value="" disabled>-------------------</option>
              <#list configurations?sort as configuration>
              	<#assign name = configuration?substring(0, configuration?last_index_of("."))>
                <option value="${configuration}">${name}</option>
              </#list>
            </select>
          </div>
          <#if errors?? && errors?seq_contains("task.program.IsBlank")>
          <div class="form-group has-error">
          <#else>
          <div class="form-group">
          </#if>
            <label for="programFile" class="control-label">${msg["programFile"]}</label>
            <input type="file" name="programFile" id="programFile">
          </div>
          <#if errors?? && errors?seq_contains("task.program.IsBlank")>
          <div class="form-group has-error">
          <#else>
          <div class="form-group">
          </#if>
            <label for="programText" class="control-label">${msg["programText"]}</label>
            <span class="help-block">${msg["programText.HelpBlock"]}</span>
            <textarea name="programText" id="programText" rows="3" class="form-control"></textarea>
          </div>
          <span class="help-block">${msg["submitTask.submissionDisclaimer"]}</span>
          <button type="submit" class="btn btn-primary">${msg["submitTask"]}</button>
        </div>
      </div>
    </div>

    <div class="col-md-6">
      <div class="panel panel-default">
        <div class="panel-heading">
          <div class="panel-title">${msg["task.options"]}</div>
        </div>
        <div class="panel-body">
          <div class="checkbox">
            <label for="disableOutput" class="control-label">
              <input type="checkbox" name="disableOutput" id="disableOutput" value="output.disable"> ${msg["option.disableOutput"]}
            </label>
          </div>
          <div class="checkbox">
            <label for="disableExportStatistics" class="control-label">
              <input type="checkbox" name="disableExportStatistics" id="disableExportStatistics" value="statistics.export"> ${msg["option.disableStatisticsExport"]}
            </label>
          </div>
          <div class="checkbox">
            <label for="dumpConfig" class="control-label">
              <input type="checkbox" name="dumpConfig" id="dumpConfig" value="configuration.dumpFile"> ${msg["option.dumpConfig"]}
            </label>
          </div>
          <div class="form-group">
            <label for="logLevel" class="control-label">${msg["option.logLevel"]}</label>
            <select name="logLevel" id="logLevel" class="form-control input-sm">
              <option value="ALL">ALL</option>
              <option value="FINEST">FINEST</option>
              <option value="FINER">FINER</option>
              <option value="FINE">FINE</option>
              <option value="INFO" selected>INFO</option>
              <option value="WARNING">WARNING</option>
              <option value="SEVERE">SEVERE</option>
              <option value="OFF">OFF</option>
            </select>
          </div>
          <div class="form-group">
            <label for="machineModel" class="control-label">${msg["option.machineModel"]}</label>
            <select name="machineModel" id="machineModel" class="form-control input-sm">
              <option value="Linux32" selected>Linux32</option>
              <option value="Linux64">Linux64</option>
            </select>
          </div>
          <div class="form-group">
            <label for="instanceType" class="control-label">${msg["option.instanceType"]}</label>
            <select name="instanceType" id="instanceType" class="form-control input-sm">
              <option value="FRONTEND" selcted>FRONTEND</option>
              <option value="BACKEND">BACKEND</option>
            </select>
          </div>
          <div class="form-group">
            <label for="wallTime" class="control-label">${msg["option.wallTime"]}</label>
            <span class="help-block">${msg["option.wallTime.info"]}</span>
            <input type="text" name="wallTime" id="wallTime" class="form-control input-sm" value="${allowedOptions['limits.time.wall']}" />
          </div>
        </div>
      </div>
    </div>
  </form>
</div>

<hr>

<div class="row">
  
  <div class="col-md-12">
    <div class="panel panel-default">
        <div class="panel-heading">
          <div class="panel-title">${msg["unsupportedFeatures"]}</div>
        </div>
        <div class="panel-body">
          <p>${msg["unsupportedFeatures.Description"]}</p>
          <ul>
            ${msg["unsupportedFeatures.ListItems"]}
          </ul>
        </div>
      </div>
  </div>
</div>

<#include "_footer.ftl">