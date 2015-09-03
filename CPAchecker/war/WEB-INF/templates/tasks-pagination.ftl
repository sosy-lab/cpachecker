<#--
  code 'borrowed' from
  http://stackoverflow.com/questions/6391668/freemarker-pagination-or-just-general-algorithm-for-clicking-through-pages
-->
<#function max x y>
    <#if (x<y)><#return y><#else><#return x></#if>
</#function>
<#function min x y>
    <#if (x<y)><#return x><#else><#return y></#if>
</#function>
<#macro pages totalPages p>
    <#assign size = totalPages?size>
    <#if (p<=threshold-1)> <#-- p among first threshold pages -->
        <#assign interval = 0..(min(threshold,size))>
    <#elseif ((size-p)<threshold)> <#-- p among last threshold pages -->
        <#assign interval = (max(1,(size-threshold)))..size >
    <#else>
        <#assign interval = (p-2)..(p+2)>
    </#if>
    <#if !(interval?seq_contains(1))>
     <li><a href="/tasks?offset=0&limit=${limit}">1</a></li>
     <li class="disabled"><span>&hellip;</span></li>
     <#rt>
    </#if>
    <#list interval as page>
        <#if page==p>
         <li class="active"><span>${page+1}</span></span></li>
         <#t>
        <#else>
          <#assign off = page * limit>
         <li><a href="/tasks?offset=${off}&limit=${limit}">${page+1}</a></li>
         <#t>
        </#if>
    </#list>
    <#if !(interval?seq_contains(size))>
     <#assign off = maxPages*limit>
     <li class="disabled"><span>&hellip;</span></li>
     <li><a href="/tasks?offset=${off}&limit=${limit}">${size+1}</a></li>
     <#lt>
    </#if>
</#macro>

<#if (numberOfTotalTasks > limit) && (limit > 0)>

<#assign currentPage = (offset/limit)?round>
<#assign maxPages = (numberOfTotalTasks/limit)?round>
<#assign nextOffset = offset + limit>
<#assign prevOffset = offset - limit>
<#assign threshold = 5>

<div class="centered">
<ul class="pagination">
  <#if (currentPage > 0)>
    <li><a href="/tasks?offset=${prevOffset}&limit=${limit}">&laquo;</a></li>
  </#if>
  <@pages 0..maxPages-1 currentPage />
  <#if (currentPage < maxPages - 1)>
    <li><a href="/tasks?offset=${nextOffset}&limit=${limit}">&raquo;</a></li>
  </#if>
</ul>
</div>
</#if>
