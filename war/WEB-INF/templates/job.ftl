<#include "_header.ftl">

<div class="container">

<div class="row">
<div class="col-md-6">
  <div class="panel panel-default">
    <div class="panel-heading">
      <div class="panel-title">Status</div>
    </div>
    <div class="panel-body">

      <table class="table-condensed">
        <tr>
          <td>Status</td>
          <td>
            <#if job.status == "PENDING">
            <span class="label label-default">${job.status}</span>
            <#elseif job.status == "RUNNING">
            <span class="label label-info">${job.status}</span>
            <#elseif job.status == "ABORTED">
            <span class="label label-warning">${job.status}</span>
            <#elseif job.status == "TIMEOUT">
            <span class="label label-danger">${job.status}</span>
            <#else>
            <span class="label label-success">${job.status}</span>
            </#if>
            <#if job.status != "DONE">
            <a href="/jobs/${job.key}">Refresh to get status update</a>
            </#if>
          </td>
        </tr>
        <#if job.resultOutcome??>
        <tr>
          <td>Outcome</td>
          <td>
            <#if job.resultOutcome == "NOT_YET_STARTED">
            <span class="label label-default">${job.resultOutcome}</span>
            <#elseif job.resultOutcome == "UNKNOWN">
            <span class="label label-warning">${job.resultOutcome}</span>
            <#elseif job.resultOutcome == "UNSAFE">
            <span class="label label-danger">${job.resultOutcome}</span>
            <#else>
            <span class="label label-success">${job.resultOutcome}</span>
            </#if>
          </td>
        </tr>
        </#if>
        <#if job.resultMessage??>
        <tr>
          <td>Message</td>
          <td>${job.resultMessage}</td>
        </tr>
        </#if>
        <tr>
          <td>Creation Date</td>
          <td>${job.creationDate?datetime}</td>
        </tr>
        <#if job.executionDate??>
        <tr>
          <td>Execution Date</td>
          <td>${job.executionDate?datetime}</td>
        </tr>
        </#if>
        <#if job.terminationDate??>
        <tr>
          <td>Termination Date</td>
          <td>${job.terminationDate?datetime}</td>
        </tr>
        </#if>
        <tr>
          <td>Specification</td>
          <td>${job.specification}</td>
        </tr>
        <tr>
          <td>Configuration</td>
          <td>${job.configuration}</td>
        </tr>
        <tr>
          <td>Queue Name</td>
          <td>${job.queueName}</td>
        </tr>
        <tr>
          <td>Task Name</td>
          <td>${job.taskName}</td>
        </tr>
        <tr>
          <td>Options</td>
          <td>
            <ul class="list-unstyled">
              <#list job.options?keys as option>
                <li>${option} = ${job.options[option]}</li>
              </#list>
            </ul>
          </td>
        </tr>
      </table>
    </div>
  </div>
</div>

<div class="col-md-6">
  <div class="panel panel-default">
    <div class="panel-heading">
      <div class="panel-title">Files</div>
    </div>
    <div class="panel-body">
      <ul>
        <#list files as file>
          <#assign name = file.path?substring(file.path?last_index_of("/")+1)>
          <li>
            <a href="/jobs/${job.key}/files/${file.key}">${name}</a>
          </li>
        </#list>
      </ul>
    </div>
  </div>
</div>
</div>

</div>

<#include "_footer.ftl">