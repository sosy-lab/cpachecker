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
            <#if task.status == "PENDING">
            <span class="label label-default">${task.status}</span>
            <a href="/tasks/${task.key}">${msg.statusRefresh}</a>
            <#elseif task.status == "RUNNING">
            <span class="label label-info">${task.status}</span>
            <a href="/tasks/${task.key}">${msg.statusRefresh}</a>
            <#elseif task.status == "TIMEOUT">
            <span class="label label-danger">${task.status}</span>
            <#elseif task.status == "ERROR">
            <span class="label label-danger">${task.status}</span>
            <#else>
            <span class="label label-success">${task.status}</span>
            </#if>
          </td>
        </tr>
        <#if task.statusMessage??>
        <tr>
        	<td>${msg.statusMessage}</td>
        	<td>${task.statusMessage}</td>
        </tr>
        </#if>
        <#if (task.retries > 0 )>
        <tr>
        	<td>${msg.retries}</td>
        	<td>${task.retries}</td>
        </tr>
        </#if>
        <#if task.resultOutcome??>
        <tr>
          <td>${msg.outcome}</td>
          <td>
            <#if task.resultOutcome == "NOT_YET_STARTED">
            <span class="label label-default">${task.resultOutcome}</span>
            <#elseif task.resultOutcome == "UNKNOWN">
            <span class="label label-warning">${task.resultOutcome}</span>
            <#elseif task.resultOutcome == "FALSE">
            <span class="label label-danger">${task.resultOutcome}</span>
            <#else>
            <span class="label label-success">${task.resultOutcome}</span>
            </#if>
          </td>
        </tr>
        </#if>
        <#if task.resultMessage??>
        <tr>
          <td>${msg.message}</td>
          <td>${task.resultMessage}</td>
        </tr>
        </#if>
        <tr>
          <td>${msg.creationDate}</td>
          <td>${task.creationDate?datetime}</td>
        </tr>
        <#if task.executionDate??>
        <tr>
          <td>${msg.executionDate}</td>
          <td>${task.executionDate?datetime}</td>
        </tr>
        </#if>
        <#if task.terminationDate??>
        <tr>
          <td>${msg.terminationDate}</td>
          <td>${task.terminationDate?datetime}</td>
        </tr>
        </#if>
        <#if task.specification??>
        <tr>
          <td>${msg.specification}</td>
          <td>${task.specification?html}</td>
        </tr>
        </#if>
        <#if task.configuration??>
        <tr>
          <td>${msg.configuration}</td>
          <td>${task.configuration?html}</td>
        </tr>
        </#if>
        <#if task.sourceFileName??>
        <tr>
          <td>${msg.sourceFileName}</td>
          <td>${task.program.name?html}</td>
        </tr>
        </#if>
        <#if task.instanceType??>
        <tr>
          <td>${msg.instanceType}</td>
          <td>${task.instanceType}</td>
        </tr>
        </#if>
        <#if task.options?? >
        <tr>
          <td>${msg.options}</td>
          <td>
            <table class="table-condensed">
              <tbody>
                <#list task.options?keys as option>
                <tr>
                  <td>${option}</td>
                  <td>${task.options[option]?html}</td>
                </tr>
                </#list>
              </tbody>
            </table>
          </td>
        </tr>
        </#if>
        <#if task.statistic?? >
        <tr>
          <td>${msg.statistic}</td>
          <td>
            <table class="table-condensed">
          	<#if task.statistic.startTime?has_content>
              <tr>
          		  <td>${msg.startTime}</td>
                <td>${(task.statistic.startTime/1000)?number_to_datetime}</td>
              </tr>
          	</#if>
          	<#if task.statistic.endTime?has_content>
              <tr>
          		  <td>${msg.endTime}</td>
                <td>${(task.statistic.endTime/1000)?number_to_datetime}</td>
              </tr>
          	</#if>
          	<#if task.statistic.latency?has_content>
              <tr>
          		  <td>${msg.latency}</td>
                <td>${task.statistic.latency/1000000} ${msg.seconds}</td>
              </tr>
          	</#if>
          	<#if task.statistic.pendingTime?has_content>
              <tr>
          		  <td>${msg.pendingTime}</td>
                <td>${task.statistic.pendingTime/1000000} ${msg.seconds}</td>
              </tr>
          	</#if>
          	<#if task.statistic.mcyclesInSeconds?has_content>
              <tr>
          		  <td>${msg.machineCyclesInSeconds}</td>
                <td>${task.statistic.mcyclesInSeconds} ${msg.seconds}</td>
              </tr>
          	</#if>
          	<#if (task.statistic.cost > 0)>
              <tr>
          		  <td>${msg.estimatedCosts}</td>
                <td>${task.statistic.cost?string("##.##")} USD</td>
              </tr>
          	</#if>
          	<#if task.statistic.host?has_content>
              <tr>
          		  <td>${msg.host}</td>
                <td>${task.statistic.host}</td>
              </tr>
          	</#if>
          	</table>
          </td>
        </tr>
        </#if>
      </table>
      <hr />
      <form action="/tasks/${task.key}?method=delete" method="post" style="display:inline">
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
            <a href="/tasks/${task.key}/files/${file.key}">${file.name?html}</a>
          </li>
        </#list>
      </ul>
    </div>
  </div>
</div>
</div>

</div>

<#include "_footer.ftl">
