<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en"><head>
  
  <script type="text/javascript">
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-24573940-1']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();
  </script>
  
  <meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />

  
  <meta name="description" content="" />

  
  <meta name="keywords" content="" />

  
  <meta name="author" content="Martin G&uuml;tlein / Original design: Andreas Viklund - http://andreasviklund.com/" />

  
  <link rel="stylesheet" type="text/css" href="andreas00.css" media="screen,projection" />
  <title>CheS-Mapper - Download</title>

<link rel="shortcut icon" type="image/x-icon" href="favicon.ico">    
</head><body>
<div id="wrap">
<div id="header">
<h1>CheS-Mapper<br/></h1>
<p><strong>Chemical Space Mapping and Visualization in 3D</strong></p>
</div>
<div id="avmenu">
<h2 class="hide">Site menu:</h2>
<ul>
  <li><a href=".">Overview</a></li>
  <li><a href="../ches-mapper-wiki">Documentation&nbsp;&nbsp;<img src="iconExternalLink.gif" /img><br /></a></li>
  <li><a href="download.html">Download</a></li>
  <li><a href="https://github.com/mguetlein/ches-mapper">Source Code&nbsp;&nbsp;<img src="iconExternalLink.gif" /img><br />
    </a></li>
  <li><a href="acknowledgements.html">Acknowledgements</a></li>
  <li><a href="contact.html">Contact</a></li>
</ul>
</div>
<div id="contentwide">

<h2>Download</h2>
<p>The current version is avaiable for download here:<br/> <a href="release/latest/ches-mapper-complete.jar">ches-mapper-complete.jar</a>
<small><b>
<?php
# to block warnings
function handleError($errno, $errstr, $errfile, $errline, array $errcontext)
{
    if (0 === error_reporting()) {
        return false;
    }
    throw new ErrorException($errstr, 0, $errno, $errfile, $errline);
}
set_error_handler('handleError');
# read version string
$version_string = "";
try {
  $file = "release/latest/VERSION";
  $f = fopen($file, 'r');
  $time = date ("d M Y",filemtime($file));
  $version = trim(fread($f,filesize($file)));
  fclose($f);
  $version_string = "$version, last updated $time";
} catch (Exception $e) {}
# read count string
$count_string = "";
try {
  $file = "RUNCOUNT";
  $f = fopen($file, 'r');
  $count = trim(fread($f,filesize($file)));
  $count = preg_split( '/;/', $count );
  $count = $count{1};
  fclose($f);
  if ((int)$count<1 || strlen($count)>10){
    throw new Exception('count invalid');
  }
  $count_string = "downloaded $count times";
  if (strlen($version_string)>0) {
    $version_string = "$version_string, ";
  }
} catch (Exception $e) {}
if (strlen($version_string)>0 || strlen($count_string)>0){
  $string = " ( $version_string$count_string )";
}
else {
  $string = "";
}
echo $string; 
?>
</b></small>
<br /><br />
Run '<span style="font-style: italic;">java -Xmx1g -jar ches-mapper-complete.jar</span>' (or just double click on windows) to start the program.<br/>
<br>
Older downloadables are available <a href="release">here</a>.
</p>

<h3>Run Online</h3>
<p>Click <a href="release/latest/ches-mapper.jnlp">ches-mapper.jnlp</a> to start
the latest version of the CheS-Mapper with Java Webstart.<br>
<br>
You can run older versions online <a href="release">here</a>.
</p>

<h3>Source Code</h3>
<p>The source code is available at <a href="release/latest/ches-mapper.jnlp">GitHub</a>.</p>

<br />
</div>
<div id="footer">
<p><b>Copyright 2012 Martin G&uuml;tlein</b> | Homepage design by <a href="http://andreasviklund.com">Andreas Viklund</a>.</p>
<p align="right"><a href="http://opentox.org"><img src="OT_small.png" /img></a></p>
</div>
</div>

</body></html>
