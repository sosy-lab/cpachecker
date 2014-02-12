<#include "_header.ftl">

<div class="container">

<div class="row">
<div class="col-md-6">
  <div class="panel panel-default">
    <div class="panel-heading">
      <div class="panel-title">${msg.status}</div>
    </div>
    <div class="panel-body">
      <table class="table table-condensed">
        <tr>
          <td>${msg.status}</td>
          <td>
            <#if job.status == "PENDING">
            <span class="label label-default">${job.status}</span>
            <a href="/tasks/${job.key}">${msg.statusRefresh}</a>
            <#elseif job.status == "RUNNING">
            <span class="label label-info">${job.status}</span>
            <a href="/tasks/${job.key}">${msg.statusRefresh}</a>
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
          <td>${job.specification?html}</td>
        </tr>
        </#if>
        <#if job.configuration??>
        <tr>
          <td>${msg.configuration}</td>
          <td>${job.configuration?html}</td>
        </tr>
        </#if>
        <#if job.sourceFileName??>
        <tr>
          <td>${msg.sourceFileName}</td>
          <td>${job.sourceFileName?html}</td>
        </tr>
        </#if>
        <#if job.instanceType??>
        <tr>
          <td>${msg.instanceType}</td>
          <td>${job.instanceType}</td>
        </tr>
        </#if>
        <#if job.options?? >
        <tr>
          <td>${msg.options}</td>
          <td>
            <table class="table-condensed">
              <tbody>
                <#list job.options?keys as option>
                <tr>
                  <td>${option}</td>
                  <td>${job.options[option]?html}</td>
                </tr>
                </#list>
              </tbody>
            </table>
          </td>
        </tr>
        </#if>
        <#if job.statistic?? >
        <tr>
          <td>${msg.statistic}</td>
          <td>
            <table class="table-condensed">
          	<#if job.statistic.startTime?has_content>
              <tr>
          		  <td>${msg.startTime}</td>
                <td>${(job.statistic.startTime/1000)?number_to_datetime}</td>
              </tr>
          	</#if>
          	<#if job.statistic.endTime?has_content>
              <tr>
          		  <td>${msg.endTime}</td>
                <td>${(job.statistic.endTime/1000)?number_to_datetime}</td>
              </tr>
          	</#if>
          	<#if job.statistic.latency?has_content>
              <tr>
          		  <td>${msg.latency}</td>
                <td>${job.statistic.latency/1000000} ${msg.seconds}</td>
              </tr>
          	</#if>
          	<#if job.statistic.pendingTime?has_content>
              <tr>
          		  <td>${msg.pendingTime}</td>
                <td>${job.statistic.pendingTime/1000000} ${msg.seconds}</td>
              </tr>
          	</#if>
          	<#if job.statistic.mcyclesInSeconds?has_content>
              <tr>
          		  <td>${msg.machineCyclesInSeconds}</td>
                <td>${job.statistic.mcyclesInSeconds} ${msg.seconds}</td>
              </tr>
          	</#if>
          	<#if (job.statistic.cost > 0)>
              <tr>
          		  <td>${msg.estimatedCosts}</td>
                <td>${job.statistic.cost?string("##.##")} USD</td>
              </tr>
          	</#if>
          	<#if job.statistic.host?has_content>
              <tr>
          		  <td>${msg.host}</td>
                <td>${job.statistic.host}</td>
              </tr>
          	</#if>
          	</table>
          </td>
        </tr>
        </#if>
      </table>
      <hr />
      <form action="/tasks/${job.key}?method=delete" method="post" style="display:inline">
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
        <#list files?sort_by("name") as file>
          <li>
            <a href="/tasks/${job.key}/files/${file.key}">${file.name?html}</a>
          </li>
        </#list>
      </ul>
    </div>
  </div>
</div>
</div>

</div>

<#include "_footer.ftl">
