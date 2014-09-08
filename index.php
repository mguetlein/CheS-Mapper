<?php include('prefix.html'); ?>

<h3>News<br />
</h3>
<p>
Check out our new <a href="https://twitter.com/CheSMapper">twitter account</a> and the new <a href="contact.php">mailing list</a>.
</p>
</h2>
<h2>Overview<br />
</h2>
<p>
CheS-Mapper (Chemical Space Mapper) is a 3D-viewer for chemical datasets with small compounds.
It has been published here: <a href="http://www.jcheminf.com/content/4/1/7">G&uuml;tlein, Karwath and Kramer (2012)</a>. To support CheS-Mapper, please cite this article.
<br>
The tool can be used to analyze the relationship between the structure of chemical compounds, their physico-chemical properties, and biological or toxic effects. CheS-Mapper embedds a dataset into 3D space, such that compounds that have similar feature values are close to each other. It can compute a range of descriptors and supports clustering and 3D alignment.
<br> 	
It is an open-source Java application, based on the Java libraries 
<a href="http://jmol.sourceforge.net">Jmol</a>, 
<a href="http://cdk.sourceforge.net">CDK</a>, 
<a href="http://www.cs.waikato.ac.nz/ml/weka">WEKA</a>, and utilizes 
<a href="http://openbabel.org">OpenBabel</a> and <a href="http://r-project.org">R</a>.
</p>

<h3>Run Online<br />
</h3>
<p>Click here to start the latest version of the CheS-Mapper with Java Web Start:
<br /> 
<a href="release/latest/ches-mapper.jnlp"><font color="#000099"><big>>>Run CheS-Mapper<<</big></font></a>
<?php
require 'version.php';
echo $webstart_info;
?>
<br>If the online version does not start, <a href="http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki/index.php?title=FAQ_-_Frequently_asked_questions">click here for troubleshooting</a> or <a href="download.php">download CheS-Mapper.</a> 
</p>

<h3>Video Tutorial<br />
</h3>
<p>
Tutorial 1 gives a brief introduction, Tutorial 3 shows SAR analysis and visual validation.
</p>
<p>
<iframe width="290" height="170" src="http://www.youtube.com/embed/HWALyzLcjF8" frameborder="0" allowfullscreen></iframe>
<iframe width="290" height="170" src="https://www.youtube.com/embed/4qU0SWXkKUI" frameborder="0" allowfullscreen></iframe>
</p>

<?php include('suffix.html'); ?>
