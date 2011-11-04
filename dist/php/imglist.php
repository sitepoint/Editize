<?php

/**
 * A Sample PHP script for giving Editize a list of images
 * from which to choose. This simple script will produce a
 * list of all .GIF, .JPG, and .PNG files in a directory
 * and output the URLs for these images using the URL prefix
 * specified.
 *
 * Set the IMAGE_DIRECTORY, URL_PREFIX, and RECURSIVE options
 * to configure the behaviour of this script.
 *
 * A more complex system might fetch the list of images from
 * a database, along with a description (ALT tag) for each
 * image.
 */

// Set this to the directory that you want to list the images in
define('IMAGE_DIRECTORY','/disk3/sites/sitepoint.com/htdocs/graphics');
// Set this to the URL prefix required (if any). Note that if Editize is
// configured with a base URL, this doesn't need to be an absolute URL.
define('URL_PREFIX','/graphics/');
// Set this 'true' to look for images in subdirectories
define('RECURSIVE',true);

header("Content-Type: text/plain");

echo_images_in_dir(IMAGE_DIRECTORY,RECURSIVE);

function echo_images_in_dir($dirname,$recurse,$prefix = '')
{
	$dir = dir($dirname);
	while(false !== ($entry = $dir->read()))
	{
		if ($entry == '.' or $entry == '..') continue;
		if (is_dir($dirname.'/'.$entry))
		{
			if ($recurse)
				echo_images_in_dir($dirname.'/'.$entry,$recurse,$prefix.basename($entry).'/');
		}
		elseif (is_image($dirname.'/'.$entry))
			echo URL_PREFIX.$prefix.$entry."\n";
	}
}

function is_image($filename)
{
	$ext = strtolower(substr($filename,-4));
	return is_file($filename) and is_readable($filename) and
		($ext == '.gif' or $ext == '.jpg' or $ext == '.png');
}
?>