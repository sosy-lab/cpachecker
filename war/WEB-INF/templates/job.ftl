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
        <#if (job.retries > 0 )>
        <tr>
        	<td>${msg.retries}</td>
        	<td>${job.retries}</td>
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
        <#if job.sourceFileName??>
        <tr>
          <td>${msg.sourceFileName}</td>
          <td>${job.sourceFileName}</td>
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
        <#if job.statistic?? >
        <tr>
          <td>${msg.statistic}</td>
          <td>
          	<ul class="list-unstyled">
          	<#if job.statistic.startTime?has_content>
          		<li>${msg.startTime}: ${(job.statistic.startTime/1000)?number_to_datetime}</li>
          	</#if>
          	<#if job.statistic.endTime?has_content>
          		<li>${msg.endTime}: ${(job.statistic.endTime/1000)?number_to_datetime}</li>
          	</#if>
          	<#if job.statistic.latency?has_content>
          		<li>${msg.latency}: ${job.statistic.latency/1000000} ${msg.seconds}</li>
          	</#if>
          	<#if job.statistic.pendingTime?has_content>
          		<li>${msg.pendingTime}: ${job.statistic.pendingTime/1000000} ${msg.seconds}</li>
          	</#if>
          	<#if job.statistic.mcyclesInSeconds?has_content>
          		<li>${msg.machineCyclesInSeconds}: ${job.statistic.mcyclesInSeconds} ${msg.seconds}</li>
          	</#if>
          	<#if (job.statistic.cost > 0)>
          		<li>${msg.estimatedCosts}: ${job.statistic.cost?string("##.##")} USD</li>
          	</#if>
          	<#if job.statistic.host?has_content>
          		<li>${msg.host}: ${job.statistic.host}</li>
          	</#if>
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
