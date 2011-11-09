<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  
  

  


  

  <head>
    <title>
      /trunk/README.txt – CPAchecker
    </title>
        <link rel="search" href="/trac/cpachecker/search" />
        <link rel="help" href="/trac/cpachecker/wiki/TracGuide" />
        <link rel="alternate" href="/trac/cpachecker/browser/trunk/README.txt?format=txt" type="text/plain" title="Plain Text" /><link rel="alternate" href="/trac/cpachecker/export/4783/trunk/README.txt" type="text/plain; charset=utf-8" title="Original Format" />
        <link rel="up" href="/trac/cpachecker/browser/trunk" title="Parent directory" />
        <link rel="start" href="/trac/cpachecker/wiki" />
        <link rel="stylesheet" href="/trac/cpachecker/chrome/common/css/trac.css" type="text/css" /><link rel="stylesheet" href="/trac/cpachecker/chrome/common/css/code.css" type="text/css" /><link rel="stylesheet" href="/trac/cpachecker/chrome/common/css/browser.css" type="text/css" />
        <link rel="shortcut icon" href="/trac/cpachecker/chrome/common/trac.ico" type="image/x-icon" />
        <link rel="icon" href="/trac/cpachecker/chrome/common/trac.ico" type="image/x-icon" />
      <link type="application/opensearchdescription+xml" rel="search" href="/trac/cpachecker/search/opensearch" title="Search CPAchecker" />
    <script type="text/javascript" src="/trac/cpachecker/chrome/common/js/jquery.js"></script><script type="text/javascript" src="/trac/cpachecker/chrome/common/js/trac.js"></script><script type="text/javascript" src="/trac/cpachecker/chrome/common/js/search.js"></script>
    <!--[if lt IE 7]>
    <script type="text/javascript" src="/trac/cpachecker/chrome/common/js/ie_pre7_hacks.js"></script>
    <![endif]-->
    <script type="text/javascript">
      jQuery(document).ready(function($) {
        $(".trac-toggledeleted").show().click(function() {
                  $(this).siblings().find(".trac-deleted").toggle();
                  return false;
        }).click();
        $("#jumploc input").hide();
        $("#jumploc select").change(function () {
          this.parentNode.parentNode.submit();
        });
      });
    </script>
  </head>
  <body>
    <div id="banner">
      <div id="header">
        <a id="logo" href="http://www.sosy-lab.org"><img src="http://www.sosy-lab.org/images/sosylogo.png" alt="Software Systems Lab" /></a>
      </div>
      <form id="search" action="/trac/cpachecker/search" method="get">
        <div>
          <label for="proj-search">Search:</label>
          <input type="text" id="proj-search" name="q" size="18" value="" />
          <input type="submit" value="Search" />
        </div>
      </form>
      <div id="metanav" class="nav">
    <ul>
      <li class="first">logged in as mcjakobs</li><li><a href="/trac/cpachecker/prefs">Preferences</a></li><li><a href="/trac/cpachecker/wiki/TracGuide">Help/Guide</a></li><li><a href="/trac/cpachecker/about">About Trac</a></li><li class="last"><a href="/trac/cpachecker/logout">Logout</a></li>
    </ul>
  </div>
    </div>
    <div id="mainnav" class="nav">
    <ul>
      <li class="first"><a href="/trac/cpachecker/wiki">Wiki</a></li><li><a href="/trac/cpachecker/timeline">Timeline</a></li><li><a href="/trac/cpachecker/roadmap">Roadmap</a></li><li class="active"><a href="/trac/cpachecker/browser">Browse Source</a></li><li><a href="/trac/cpachecker/report">View Tickets</a></li><li><a href="/trac/cpachecker/newticket">New Ticket</a></li><li class="last"><a href="/trac/cpachecker/search">Search</a></li>
    </ul>
  </div>
    <div id="main">
      <div id="ctxtnav" class="nav">
        <h2>Context Navigation</h2>
          <ul>
              <li class="first"><a href="/trac/cpachecker/changeset/4666/trunk/README.txt">Last Change</a></li><li><a href="/trac/cpachecker/browser/trunk/README.txt?annotate=blame&amp;rev=4666" title="Annotate each line with the last changed revision (this can be time consuming...)">Annotate</a></li><li class="last"><a href="/trac/cpachecker/log/trunk/README.txt">Revision Log</a></li>
          </ul>
        <hr />
      </div>
    <div id="content" class="browser">
      <h1>
    <a class="pathentry first" title="Go to root directory" href="/trac/cpachecker/browser">root</a><span class="pathentry sep">/</span><a class="pathentry" title="View trunk" href="/trac/cpachecker/browser/trunk">trunk</a><span class="pathentry sep">/</span><a class="pathentry" title="View README.txt" href="/trac/cpachecker/browser/trunk/README.txt">README.txt</a>
    <br style="clear: both" />
  </h1>
      <div id="jumprev">
        <form action="" method="get">
          <div>
            <label for="rev">
              View revision:</label>
            <input type="text" id="rev" name="rev" size="6" />
          </div>
        </form>
      </div>
      <div id="jumploc">
        <form action="" method="get">
          <div class="buttons">
            <label for="preselected">Visit:</label>
            <select id="preselected" name="preselected">
              <option selected="selected"></option>
              <optgroup label="branches">
                <option value="/trac/cpachecker/browser/trunk">trunk</option><option value="/trac/cpachecker/browser/branches/abe">branches/abe</option><option value="/trac/cpachecker/browser/branches/cfa">branches/cfa</option><option value="/trac/cpachecker/browser/branches/fshell3">branches/fshell3</option><option value="/trac/cpachecker/browser/branches/interpreter-ds">branches/interpreter-ds</option><option value="/trac/cpachecker/browser/branches/ivy">branches/ivy</option><option value="/trac/cpachecker/browser/branches/multi-threaded">branches/multi-threaded</option><option value="/trac/cpachecker/browser/branches/pcc">branches/pcc</option><option value="/trac/cpachecker/browser/branches/precision-refinement">branches/precision-refinement</option>
              </optgroup><optgroup label="tags">
                <option value="/trac/cpachecker/browser/tags/cav11?rev=3433">tags/cav11</option><option value="/trac/cpachecker/browser/tags/cpachecker-0.8?rev=1446">tags/cpachecker-0.8</option><option value="/trac/cpachecker/browser/tags/cpachecker-0.9?rev=1446">tags/cpachecker-0.9</option><option value="/trac/cpachecker/browser/tags/cpachecker-1.0?rev=2689">tags/cpachecker-1.0</option>
              </optgroup>
            </select>
            <input type="submit" value="Go!" title="Jump to the chosen preselected path" />
          </div>
        </form>
      </div>
      <table id="info" summary="Revision info">
        <tr>
          <th scope="col">
            Revision <a href="/trac/cpachecker/changeset/4666">4666</a>, <span title="3246 bytes">3.2 KB</span>
            (checked in by pwendler, <a class="timeline" href="/trac/cpachecker/timeline?from=2011-11-02T07%3A18%3A17-0700&amp;precision=second" title="2011-11-02T07:18:17-0700 in Timeline">7 days</a> ago)
          </th>
        </tr>
        <tr>
          <td class="message searchable">
              <p>
Move default output directory of CPAchecker from test/output/ to output/.<br />
</p>
<p>
When a user runs CPAchecker, the results of CPAchecker have nothing to do with "test"s.<br />
</p>
          </td>
        </tr>
      </table>
      <div id="preview" class="searchable">
    <table class="code"><thead><tr><th class="lineno" title="Line numbers">Line</th><th class="content"> </th></tr></thead><tbody><tr><th id="L1"><a href="#L1">1</a></th><td>Getting Started with CPAchecker</td></tr><tr><th id="L2"><a href="#L2">2</a></th><td>===============================</td></tr><tr><th id="L3"><a href="#L3">3</a></th><td></td></tr><tr><th id="L4"><a href="#L4">4</a></th><td>Installation Instructions:  INSTALL.txt</td></tr><tr><th id="L5"><a href="#L5">5</a></th><td>Develop and Contribute:     CONTRIB.txt</td></tr><tr><th id="L6"><a href="#L6">6</a></th><td></td></tr><tr><th id="L7"><a href="#L7">7</a></th><td>More details can be found in doc/*.txt</td></tr><tr><th id="L8"><a href="#L8">8</a></th><td></td></tr><tr><th id="L9"><a href="#L9">9</a></th><td></td></tr><tr><th id="L10"><a href="#L10">10</a></th><td>Prepare Programs for Verification by CPAchecker</td></tr><tr><th id="L11"><a href="#L11">11</a></th><td>-----------------------------------------------</td></tr><tr><th id="L12"><a href="#L12">12</a></th><td></td></tr><tr><th id="L13"><a href="#L13">13</a></th><td>   Sources have to be preprocessed by CIL</td></tr><tr><th id="L14"><a href="#L14">14</a></th><td>   (http://hal.cs.berkeley.edu/cil/, mirror at http://www.cs.berkeley.edu/~necula/cil/).</td></tr><tr><th id="L15"><a href="#L15">15</a></th><td>   Necessary flags:</td></tr><tr><th id="L16"><a href="#L16">16</a></th><td>   --dosimplify --printCilAsIs --save-temps --domakeCFG</td></tr><tr><th id="L17"><a href="#L17">17</a></th><td>   Possibly necessary flags:</td></tr><tr><th id="L18"><a href="#L18">18</a></th><td>   --dosimpleMem</td></tr><tr><th id="L19"><a href="#L19">19</a></th><td>   Comments:</td></tr><tr><th id="L20"><a href="#L20">20</a></th><td>   --save-temps saves files to the current directory, a different directory can</td></tr><tr><th id="L21"><a href="#L21">21</a></th><td>   be specified by using --save-temps=&lt;DIRECTORY&gt;</td></tr><tr><th id="L22"><a href="#L22">22</a></th><td></td></tr><tr><th id="L23"><a href="#L23">23</a></th><td></td></tr><tr><th id="L24"><a href="#L24">24</a></th><td>Verifying a Program with CPAchecker</td></tr><tr><th id="L25"><a href="#L25">25</a></th><td>-----------------------------------</td></tr><tr><th id="L26"><a href="#L26">26</a></th><td></td></tr><tr><th id="L27"><a href="#L27">27</a></th><td>1. Choose a source code file that you want to be checked.</td></tr><tr><th id="L28"><a href="#L28">28</a></th><td>   Several types of example programs can be found in test/programs/</td></tr><tr><th id="L29"><a href="#L29">29</a></th><td>   If you use your own program, remember to pre-process it with CIL (see above).</td></tr><tr><th id="L30"><a href="#L30">30</a></th><td>   Example: test/programs/simple/loop1.c</td></tr><tr><th id="L31"><a href="#L31">31</a></th><td></td></tr><tr><th id="L32"><a href="#L32">32</a></th><td>2. If you want to enable certain analyses like predicate analysis,</td></tr><tr><th id="L33"><a href="#L33">33</a></th><td>   choose a configuration file. This file defines for example which CPAs are used.</td></tr><tr><th id="L34"><a href="#L34">34</a></th><td>   Standard configuration files can be found in the directory config/.</td></tr><tr><th id="L35"><a href="#L35">35</a></th><td>   Example: config/explicitAnalysis.properties</td></tr><tr><th id="L36"><a href="#L36">36</a></th><td>   The configuration options used in this file are explained in doc/Configuration.txt.</td></tr><tr><th id="L37"><a href="#L37">37</a></th><td></td></tr><tr><th id="L38"><a href="#L38">38</a></th><td>3. Choose a specification file (you may not need this for some CPAs).</td></tr><tr><th id="L39"><a href="#L39">39</a></th><td>   The standard configuration files use config/specification/ErrorLocation.spc</td></tr><tr><th id="L40"><a href="#L40">40</a></th><td>   as the default specification. With this one, CPAchecker will look for labels</td></tr><tr><th id="L41"><a href="#L41">41</a></th><td>   named "ERROR" and assertions in the source code file.</td></tr><tr><th id="L42"><a href="#L42">42</a></th><td>   Other examples for specifications can be found in config/specification/</td></tr><tr><th id="L43"><a href="#L43">43</a></th><td></td></tr><tr><th id="L44"><a href="#L44">44</a></th><td>4. Execute "scripts/cpa.sh [ -config &lt;CONFIG_FILE&gt; ] [ -spec &lt;SPEC_FILE&gt; ] &lt;SOURCE_FILE&gt;"</td></tr><tr><th id="L45"><a href="#L45">45</a></th><td>   Either a configuration file or a specification file needs to be given.</td></tr><tr><th id="L46"><a href="#L46">46</a></th><td>   The current directory should be the CPAchecker project directory.</td></tr><tr><th id="L47"><a href="#L47">47</a></th><td>   Additional command line switches are described in doc/Configuration.txt.</td></tr><tr><th id="L48"><a href="#L48">48</a></th><td>   Example: scripts/cpa.sh -config config/explicitAnalysis.properties test/programs/simple/loop1.c</td></tr><tr><th id="L49"><a href="#L49">49</a></th><td>   This example can also be abbreviated to:</td></tr><tr><th id="L50"><a href="#L50">50</a></th><td>   scripts/cpa.sh -explicitAnalysis test/programs/simple/loop1.c</td></tr><tr><th id="L51"><a href="#L51">51</a></th><td></td></tr><tr><th id="L52"><a href="#L52">52</a></th><td>5. Additionally to the console output, there will be several files in the directory output/:</td></tr><tr><th id="L53"><a href="#L53">53</a></th><td>     ART.dot: Visualization of abstract reachability tree (Graphviz format)</td></tr><tr><th id="L54"><a href="#L54">54</a></th><td>     cfa*.dot: Visualization of control flow automaton (Graphviz format)</td></tr><tr><th id="L55"><a href="#L55">55</a></th><td>     counterexample.msat: Formula representation of the error path</td></tr><tr><th id="L56"><a href="#L56">56</a></th><td>     ErrorPath.txt: A path through the program that leads to an error</td></tr><tr><th id="L57"><a href="#L57">57</a></th><td>     ErrorPathAssignment.txt: Assignments for all variables on the error path.</td></tr><tr><th id="L58"><a href="#L58">58</a></th><td>     predmap.txt: Predicates used by predicate analysis to prove program safety</td></tr><tr><th id="L59"><a href="#L59">59</a></th><td>     reached.txt: Dump of all reached abstract states</td></tr><tr><th id="L60"><a href="#L60">60</a></th><td>     Statistics.txt: Time statistics (can also be printed to console with "-stats")</td></tr><tr><th id="L61"><a href="#L61">61</a></th><td>   Note that not all of these files will be available for all configurations.</td></tr><tr><th id="L62"><a href="#L62">62</a></th><td>   Also some of these files are only produced if an error is found (or vice-versa).</td></tr><tr><th id="L63"><a href="#L63">63</a></th><td>   CPAchecker will overwrite files in this directory!</td></tr><tr><th id="L64"><a href="#L64">64</a></th><td>   These files may be used to generate a report that can be viewed in a browser.</td></tr><tr><th id="L65"><a href="#L65">65</a></th><td>   Cf. BuildReport.txt for this.</td></tr></tbody></table>
      </div>
      <div id="help">
        <strong>Note:</strong> See <a href="/trac/cpachecker/wiki/TracBrowser">TracBrowser</a>
        for help on using the browser.
      </div>
      <div id="anydiff">
        <form action="/trac/cpachecker/diff" method="get">
          <div class="buttons">
            <input type="hidden" name="new_path" value="/trunk/README.txt" />
            <input type="hidden" name="old_path" value="/trunk/README.txt" />
            <input type="hidden" name="new_rev" />
            <input type="hidden" name="old_rev" />
            <input type="submit" value="View changes..." title="Select paths and revs for Diff" />
          </div>
        </form>
      </div>
    </div>
    <div id="altlinks">
      <h3>Download in other formats:</h3>
      <ul>
        <li class="first">
          <a rel="nofollow" href="/trac/cpachecker/browser/trunk/README.txt?format=txt">Plain Text</a>
        </li><li class="last">
          <a rel="nofollow" href="/trac/cpachecker/export/4783/trunk/README.txt">Original Format</a>
        </li>
      </ul>
    </div>
    </div>
    <div id="footer" lang="en" xml:lang="en"><hr />
      <a id="tracpowered" href="http://trac.edgewall.org/"><img src="/trac/cpachecker/chrome/common/trac_logo_mini.png" height="30" width="107" alt="Trac Powered" /></a>
      <p class="left">
        Powered by <a href="/trac/cpachecker/about"><strong>Trac 0.11.7</strong></a><br />
        By <a href="http://www.edgewall.org/">Edgewall Software</a>.
      </p>
      <p class="right">Visit the Trac open source project at<br /><a href="http://trac.edgewall.org/">http://trac.edgewall.org/</a></p>
    </div>
  </body>
</html>