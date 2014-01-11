<#include "_header.ftl">

<div class="container">

<div class="row">
  <div class="col-md-12">
    <div class="panel panel-default">
      <div class="panel-heading">
        <div class="panel-title">${msg.allJobs}</div>
      </div>
      <div class="panel-body">
        <ul>
        <#list jobs?sort_by("creationDate")?reverse as job>
          <li>
            <a href="/jobs/${job.key}">${job.creationDate?datetime}</a>

            <#if job.status == "PENDING">
              <span class="label label-default">${job.status}</span>
            <#elseif job.status == "RUNNING">
              <span class="label label-info">${job.status}</span>
            <#elseif job.status == "ABORTED">
              <span class="label label-warning">${job.status}</span>
            <#elseif job.status == "TIMEOUT">
              <span class="label label-danger">${job.status}</span>
            <#elseif job.status == "ERROR">
              <span class="label label-danger">${job.status}</span>
            <#else>
              <span class="label label-success">${job.status}</span>
            </#if>
            
            <#if job.resultOutcome??>
            <#if job.resultOutcome == "NOT_YET_STARTED">
              <span class="label label-default">${job.resultOutcome}</span>
            <#elseif job.resultOutcome == "UNKNOWN">
              <span class="label label-warning">${job.resultOutcome}</span>
            <#elseif job.resultOutcome == "UNSAFE">
              <span class="label label-danger">${job.resultOutcome}</span>
            <#else>
              <span class="label label-success">${job.resultOutcome}</span>
            </#if>
            </#if>
            
            <form action="/jobs/${job.key}?method=delete" method="post" style="display:inline">
            	<button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span></button>
            </form>
          </li>
        </#list>
        </ul>
    </div>
  </div>
</div>

<ul>

</div>

<#include "_footer.ftl">