<?php include('prefix.html'); ?>

<h2>Download</h2>

<?php
require 'version.php';
echo $download_info;
?>

<UL>
<LI><a href="release/latest/ches-mapper-complete.jar">ches-mapper-complete.jar</a> (Executable jar file, platform independent)</LI>
<LI><a href="release/latest/ches-mapper.exe">ches-mapper.exe</a> (Windows executable)</LI>
</UL>
<p>
Please find a description on how to run the software in the <a href="http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki/index.php?title=FAQ_-_Frequently_asked_questions">documentation</a>.<br>Previous versions are available <a href="release">here</a>.
</p>

<h3>Run Online</h3>
<p>On the <a href="index.php">overview</a> page, you can start CheS-Mapper directly with the browser with Java Web Start.
</p>

<h3>Source Code</h3>
<p>The source code is available at <a href="release/latest/ches-mapper.jnlp">GitHub</a>.</p>

<br />
<?php include('suffix.html'); ?>
