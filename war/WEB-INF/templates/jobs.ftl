<#include "_header.ftl">

<div class="container">

<div class="row">
  <div class="col-md-12">
    <div class="panel panel-default">
      <div class="panel-heading">
        <div class="panel-title">${msg.allJobs}</div>
      </div>
      <div class="panel-body">
        <table class="table table-bordered table-condensed table-hover table-jobs">
            <tr>
              <th>${msg.creationDate}</th>
              <th>${msg.status}</th>
              <th>${msg.outcome}</th>
              <th>${msg.delete}</th>
            </tr>
          </thead>
          <tbody>
          <#list jobs?sort_by("creationDate")?reverse as job>

          <#if job.status == "PENDING">
            <#assign statusLabel = "default">
          <#elseif job.status == "RUNNING">
            <#assign statusLabel = "info">
          <#elseif job.status == "ABORTED">
            <#assign statusLabel = "warning">
          <#elseif job.status == "TIMEOUT">
            <#assign statusLabel = "danger">
          <#elseif job.status == "ERROR">
            <#assign statusLabel = "danger">
          <#else>
            <#assign statusLabel = "success">
          </#if>

          <#if job.resultOutcome??>
            <#if job.resultOutcome == "NOT_YET_STARTED">
              <#assign outcomeLabel = "default">
            <#elseif job.resultOutcome == "UNKNOWN">
              <#assign outcomeLabel = "warning">
            <#elseif job.resultOutcome == "FALSE">
              <#assign outcomeLabel = "danger">
            <#else>
              <#assign outcomeLabel = "success">
            </#if>
          </#if>

          <tr>
            <td>
              <a href="/tasks/${job.key}" class="hover-decorate">${job.creationDate?string("yyyy-MM-dd @ HH:mm:ss")}</a>
            </td>
            <td>
              <a href="/tasks/${job.key}">
                <span class="label label-${statusLabel}">${job.status}</span>
              </a>
            </td>
            <td>
              <a href="/tasks/${job.key}">
              <#if job.resultOutcome??>
                <span class="label label-${outcomeLabel}">${job.resultOutcome}</span>
              <#else>
                &nbsp;
              </#if>
              </a>
            </td>
            <td>
              <form action="/tasks/${job.key}?method=delete" method="post" style="display:inline">
            	 <button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span> ${msg.delete}</button>
              </form>
            </td>
          </tr>
          </#list>
        </tbody>
      </table>
    </div>
  </div>
</div>

</div>

<#include "_footer.ftl">
