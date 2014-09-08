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
  $webstart_count = $count{0};
  $download_count = $count{1};
  fclose($f);
  if ((int)$webstart_count<1 || strlen($webstart_count)>10){
    throw new Exception('count invalid');
  }
  $webstart_count_string = "started $webstart_count times";
  $download_count_string = "downloaded $download_count times";
  if (strlen($version_string)>0) {
    $version_string = "$version_string, ";
  }
} catch (Exception $e) {}
if (strlen($version_string)>0 || strlen($count_string)>0) {
  $webstart_info = " ( $version_string$webstart_count_string )";
  $download_info = "The current version is $version_string$download_count_string:";
} else {
  $webstart_info = "";
  $download_info = "The current version is available for download here:";
}
#echo $string; 

?>