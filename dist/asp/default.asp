<%@ Language="VBScript" %>
<%
	Option Explicit
 
	' Editize API for ASP demo
	' by Kevin Yank
	'
	' This example presents a form with two instances of Editize, each
	' configured with different features enabled. When the form is
	' submitted, the results are displayed for the user to see. The
	' user is then able to click 'Edit Further' to load the two
	' documents back into the original form to demonstrate how Editize
	' can display an existing document for editing.

	' Assign some variables for the values that will be passed by the form
	' submissions in this script.
	Dim title, blurb, article
	title   = Request.Form("title")
	blurb   = Request.Form("blurb")
	article = Request.Form("article")
%>
<html>
<head>
<title>Editize API for ASP Demo</title>
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
<% If Request.Form("edited").Count > 0 Then %><base href="http://www.sitepoint.com/" /><% End If %>
</head>
<body bgcolor="#FFFFFF" text="#000000">
<%
	' Determine if the form has been submitted or not.
	If Request.Form("edited").Count < 1 Then ' The form has not been submitted
%>
<p>This sample form contains two Editize fields. It's designed to look like a
   typical form that you might see in a content management system.</p>
<form action="http://<%=Request.ServerVariables("HTTP_HOST") & Request.ServerVariables("SCRIPT_NAME")%>" method="post">
<h3>Title:</h3>
<input type="text" name="title" value="<%=Server.HTMLEncode(title)%>" size="30" />
<h3>Blurb:</h3>
<%
	' Here we create and configure our first instance of Editize, which
	' will be used for the 'Blurb' field of the form. A blurb is a short
	' piece of text that is displayed on the front page of the site as a
	' teaser for the article. Complex fomatting sych as hyperlinks, paragraph
	' styles and lists are not appropriate for this type of content, so we
	' disable all these features. We also use a font size that is a little
	' smaller than usual (12 pixels, which corresponds to the font size
	' specified for the blurb in the stylesheet for the site (see above).
	Dim blurbedit
	Set blurbedit = GetObject ("script:" & Server.MapPath ("editize.wsc"))
	' OR IF REGISTERED: Set blurbedit = Server.CreateObject("Editize.aspapi")
	blurbedit.name = "blurb"
	blurbedit.width = "100%"
	blurbedit.height = "200"
	blurbedit.paragraphstyles = FALSE
	blurbedit.paragraphalignments = FALSE
	blurbedit.bulletlists = FALSE
	blurbedit.numberedlists = FALSE
	blurbedit.hyperlinks = FALSE
	blurbedit.basefontface = "Verdana"
	blurbedit.basefontsize = "12"
	blurbedit.appletbgcolor = "white"
	blurbedit.ns4support = TRUE
	Response.Write(blurbedit.DisplayContent(blurb))
%>
<h3>Article:</h3>
<%
	' Here's our second instance of Editize. We leave all the features enabled
	' for this instance, and configure a 14 pixel font size, which matches the
	' stylesheet setting for the article text size (see above).
	' In addition, we set a base URL for images and provide an image list URL.
	Dim ed
	Set ed = GetObject ("script:" & Server.MapPath ("editize.wsc"))
	' OR IF REGISTERED: Set ed = Server.CreateObject("Editize.aspapi")
	ed.name = "article"
	ed.width = "100%"
	ed.height = "400"
	ed.basefontface = "Verdana"
	ed.basefontsize = "14"
	Dim links(2)
	links(0) = "mailto:"
	links(1) = "http://www.sitepoint.com/article.php/"
	ed.linkurls = links
	ed.codeview = TRUE
	ed.appletbgcolor = "white"
	ed.ns4support = TRUE
	Response.Write(ed.DisplayContent(article))
%><br />
<input type="hidden" name="edited" value="true" />
<input type="submit" name="submitarticle" value="Submit Article" />
</form>
<%
	Response.Write(ed.EndForm())
%>
<%
	Else ' The form has been submitted
%>
<h1><%=Server.HTMLEncode(title)%></h1>
<h3>Blurb:</h3>
<div class="blurbtext"><%=blurb%></div>
<h3>Article:</h3>
<div class="articletext"><%=article%></div>

<!-- This form will re-submit the article for editing -->
<form action="http://<%=Request.ServerVariables("HTTP_HOST") & Request.ServerVariables("SCRIPT_NAME")%>" method="POST">
<input type="hidden" name="title" value="<%=Server.HTMLEncode(title)%>" />
<input type="hidden" name="blurb" value="<%=Server.HTMLEncode(blurb)%>" />
<input type="hidden" name="article" value="<%=Server.HTMLEncode(article)%>" />
<input type="submit" name="edit" value="Edit Further" />
</form>
<%
	End If
%>
</body>
</html>
