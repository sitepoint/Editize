<?php
	/*-------------------------------------------------------------------*\
	 | Editize API for JavaScript demo                                   |
	 | by Kevin Yank                                                     |
	 |                                                                   |
	 | This example presents a form with two instances of Editize, each  |
	 | configured with different features enabled. When the form is      |
	 | submitted, the results are displayed for the user to see. The     |
	 | user is then able to click 'Edit Further' to load the two         |
	 | documents back into the original form to demonstrate how Editize  |
	 | can display an existing document for editing.                     |
	 |                                                                   |
	 | The server-side elements of this example are written in PHP;      |
	 | however, there is nothing specific to Editize in this code. This  |
	 | demonstrates that any server-side language may be used with the   |
	 | JavaScript API for Editize.                                       |
	\*-------------------------------------------------------------------*/

	/* Assign some variables for the values that will be passed by the form
	   submissions in this script. */
	$title   = isset($HTTP_POST_VARS['title']) ? $HTTP_POST_VARS['title'] : "";
	$blurb   = isset($HTTP_POST_VARS['blurb']) ? $HTTP_POST_VARS['blurb'] : "";
	$article = isset($HTTP_POST_VARS['article']) ? $HTTP_POST_VARS['article'] : "";
	
	/* So that this script will work on PHP installations with
	   magic_quotes_gpc turned on and off, we detect this setting and strip
	   off any slashes that have been added to our variables by PHP. */
	if (get_magic_quotes_gpc())
	{
		$title   = stripslashes($title);
		$blurb   = stripslashes($blurb);
		$article = stripslashes($article);
	}  
?>
<html>
<head>
<title>Editize API for JavaScript Demo</title>
<!-- The following styles reflect the formatting of the Web site, which
     Editize will be configured to emulate... -->
<style type="text/css">
p, body {
  font-family: Verdana;
  font-size: 10pt;
}
h1 {
  font-family: Arial;
  font-size: 20pt;
}
h3 {
  font-family: Arial;
  font-size: 16pt;
}
.blurbtext, .blurbtext p {
  font-family: Verdana;
  font-size: 12px;
}
.articletext, .articletext p {
  font-family: Verdana;
  font-size: 14px;
}
.highlighted { color: red; }
</style>
<script language="JavaScript" src="editize.js"></script>
<?php if (isset($HTTP_GET_VARS['submit'])): ?><base href="http://www.sitepoint.com/"><?php endif; ?>
</head>
<body bgcolor="#FFFFFF" text="#000000">
<?php
	/* Determine if the form has been submitted or not. */
	if (!isset($HTTP_POST_VARS['edited'])): // If the form has not been submitted
?>
<p>This sample form contains two Editize fields. It's designed to look like a
   typical form that you might see in a content management system.</p>
<form action="http://<?php echo $HTTP_SERVER_VARS['HTTP_HOST'] . $HTTP_SERVER_VARS['PHP_SELF']; ?>" method="post">
<h3>Title:</h3>
<input type="text" name="title" value="<?php echo htmlspecialchars($title); ?>" size="30" />
<h3>Blurb:</h3>
<!-- Here we create and configure our first instance of Editize, which
     will be used for the 'Blurb' field of the form. A blurb is a short
     piece of text that is displayed on the front page of the site as a
     teaser for the article. Complex fomatting sych as hyperlinks, paragraph
     styles and lists are not appropriate for this type of content, so we
     disable all these features. We also use a font size that is a little
     smaller than usual (12 pixels, which corresponds to the font size
     specified for the blurb in the stylesheet for the site (see above). -->
  <script language="JavaScript">
    var blurbedit = new Editize();
    blurbedit.name = 'blurb';
    blurbedit.width = '100%';
    blurbedit.height = '200';
    blurbedit.paragraphstyles = false;
    blurbedit.paragraphalignments = false;
    blurbedit.bulletlists = false;
    blurbedit.numberedlists = false;
    blurbedit.hyperlinks = false;
    blurbedit.basefontface = 'Verdana';
    blurbedit.basefontsize = '12';
    blurbedit.images = false;
    blurbedit.appletbgcolor = 'white';
    blurbedit.ns4support = true;
    blurbedit.display('<?php echo addcslashes($blurb,"\n\r\t\'\\"); ?>');
  </script>
<h3>Article:</h3>
<!-- Here's our second instance of Editize. We leave all the features enabled
     for this instance, and configure a 14 pixel font size, which matches the
     stylesheet setting for the article text size (see above).
     In addition, we set a base URL for images and provide an image list URL. -->
  <script language="JavaScript">
    var ed = new Editize();
    ed.name = 'article';
    ed.width = '100%';
    ed.height = '400';
    ed.basefontface = 'Verdana';
    ed.basefontsize = '14';
    ed.imglisturl = 'http://www.sitepoint.com/graphics/imglist.php';
    ed.baseurl = 'http://www.sitepoint.com/';
    ed.linkurls = new Array('mailto:','http://www.sitepoint.com/article.php/');
    ed.codeview = true;
    ed.appletbgcolor = 'white';
    ed.ns4support = true;
    ed.display('<?php echo addcslashes($article,"\n\r\t\'\\"); ?>');
  </script>
  <br />
  <input type="hidden" name="edited" value="true" />
  <input type="submit" name="submitarticle" value="Submit Article" />
</form>
<script language="JavaScript">
  ed.endForm();
</script>
<?php
	else: // If the form has been submitted
?>
<h1><?php echo $title; ?></h1>
<h3>Blurb:</h3>
<div class="blurbtext"><?php echo $blurb; ?></div>
<h3>Article:</h3>
<div class="articletext"><?php echo $article; ?></div>

<!-- This form will re-submit the article for editing -->
<form action="http://<?php echo $HTTP_SERVER_VARS['HTTP_HOST'] . $HTTP_SERVER_VARS['PHP_SELF']; ?>" method="POST">
<input type="hidden" name="title" value="<?php echo htmlspecialchars($title); ?>" />
<input type="hidden" name="blurb" value="<?php echo htmlspecialchars($blurb); ?>" />
<input type="hidden" name="article" value="<?php echo htmlspecialchars($article); ?>" />
<input type="submit" name="edit" value="Edit Further" />
</form>
<?php endif; ?>
</body>
</html>
