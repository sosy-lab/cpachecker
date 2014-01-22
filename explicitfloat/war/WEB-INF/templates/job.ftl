<#include "_header.ftl">

<div class="container">

<div class="row">
<div class="col-md-6">
  <div class="panel panel-default">
    <div class="panel-heading">
      <div class="panel-title">${msg.status}</div>
    </div>
    <div class="panel-body">
      <table class="table-condensed">
        <tr>
          <td>${msg.status}</td>
          <td>
            <#if job.status == "PENDING">
            <span class="label label-default">${job.status}</span>
            <a href="/jobs/${job.key}">${msg.statusRefresh}</a>
            <#elseif job.status == "RUNNING">
            <span class="label label-info">${job.status}</span>
            <a href="/jobs/${job.key}">${msg.statusRefresh}</a>
            <#elseif job.status == "TIMEOUT">
            <span class="label label-danger">${job.status}</span>
            <#elseif job.status == "ERROR">
            <span class="label label-danger">${job.status}</span>
            <#else>
            <span class="label label-success">${job.status}</span>
            </#if>
          </td>
        </tr>
        <#if job.statusMessage??>
        <tr>
        	<td>${msg.statusMessage}</td>
        	<td>${job.statusMessage}</td>
        </tr>
        </#if>
        <#if job.resultOutcome??>
        <tr>
          <td>${msg.outcome}</td>
          <td>
            <#if job.resultOutcome == "NOT_YET_STARTED">
            <span class="label label-default">${job.resultOutcome}</span>
            <#elseif job.resultOutcome == "UNKNOWN">
            <span class="label label-warning">${job.resultOutcome}</span>
            <#elseif job.resultOutcome == "FALSE">
            <span class="label label-danger">${job.resultOutcome}</span>
            <#else>
            <span class="label label-success">${job.resultOutcome}</span>
            </#if>
          </td>
        </tr>
        </#if>
        <#if job.resultMessage??>
        <tr>
          <td>${msg.message}</td>
          <td>${job.resultMessage}</td>
        </tr>
        </#if>
        <tr>
          <td>${msg.creationDate}</td>
          <td>${job.creationDate?datetime}</td>
        </tr>
        <#if job.executionDate??>
        <tr>
          <td>${msg.executionDate}</td>
          <td>${job.executionDate?datetime}</td>
        </tr>
        </#if>
        <#if job.terminationDate??>
        <tr>
          <td>${msg.terminationDate}</td>
          <td>${job.terminationDate?datetime}</td>
        </tr>
        </#if>
        <#if job.specification??>
        <tr>
          <td>${msg.specification}</td>
          <td>${job.specification}</td>
        </tr>
        </#if>
        <#if job.configuration??>
        <tr>
          <td>${msg.configuration}</td>
          <td>${job.configuration}</td>
        </tr>
        </#if>
        <#if job.options?? >
        <tr>
          <td>${msg.options}</td>
          <td>
          	<ul class="list-unstyled">
          	<#list job.options?keys as option>
          		<li>${option} = ${job.options[option]}</li>
          	</#list>
          	</ul>
          </td>
        </tr>
        </#if>
      </table>
      <hr />
      <form action="/jobs/${job.key}?method=delete" method="post" style="display:inline">
      	<button type="submit" class="btn btn-sm btn-danger"><span class="glyphicon glyphicon-trash"></span> ${msg.delete}</button>
      </form>
    </div>
  </div>
</div>

<div class="col-md-6">
  <div class="panel panel-default">
    <div class="panel-heading">
      <div class="panel-title">${msg.files}</div>
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
