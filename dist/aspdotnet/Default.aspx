<%@ Page language="VB" AutoEventWireup="True" ValidateRequest="False" %>
<%@ Register TagPrefix="SitePoint" Namespace="EditizeDotNet" Assembly="EditizeDotNet" %>
<html>
	<!--
	  -- Editize API for ASP.NET demo
	  -- by Kevin Yank
	  --
	  -- This example presents a form with two instances of Editize, each
	  -- configured with different features enabled. When the form is
	  -- submitted, the results are displayed for the user to see. The
	  -- user is then able to click 'Edit Further' to load the two
	  -- documents back into the original form to demonstrate how Editize
	  -- can display an existing document for editing.
	  -->
	<head>
		<title>Editize API for ASP.NET Demo (VB.NET)</title>
		<script runat="server">
			protected Sub FormContent_Changed(sender as object, e as EventArgs)
				' Handle form submissions by displaying the resulting document
				PreviewLabel.Visible = true
				TitleLabel.Visible = true
				BlurbLabel.Visible = true
				ArticleLabel.Visible = true
				SeparatorLabel.Visible = true
				
				TitleLabel.Text = "<h1>" + TitleTextBox.Text + "</h1>"
				BlurbLabel.Text = BlurbEditize.Content
				ArticleLabel.Text = ArticleEditize.Content
			End Sub
		</script>
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
	</head>
	<body bgcolor="#FFFFFF" text="#000000">
		<form id="Form1" method="post" runat="server">
			<asp:Label Runat="server" ID="PreviewLabel" Visible="False"><p>Here are the documents you created! Scroll down to edit them further if your like.</p></asp:Label>
			<asp:Label Runat="server" ID="TitleLabel" Visible="False" />
			<asp:Label Runat="server" ID="BlurbLabel" CssClass="blurbtext" Visible="False" />
			<asp:Label Runat="server" ID="ArticleLabel" CssClass="articletext" Visible="False" />
			<asp:Label Runat="server" ID="SeparatorLabel" Visible="False"><hr /></asp:Label>
			<p>This sample form contains two Editize fields. It's designed to look like a typical form that you might see in a content management system.</p>
			<h3>Title:</h3>
			<asp:TextBox Runat="server" ID="TitleTextBox" onTextChanged="FormContent_Changed" />
			<h3>Blurb:</h3>
			<!-- Here we create and configure our first instance of Editize, which
			  -- will be used for the 'Blurb' field of the form. A blurb is a short
			  -- piece of text that is displayed on the front page of the site as a
			  -- teaser for the article. Complex fomatting sych as hyperlinks, paragraph
			  -- styles and lists are not appropriate for this type of content, so we
			  -- disable all these features. We also use a font size that is a little
			  -- smaller than usual (12 pixels, which corresponds to the font size
			  -- specified for the blurb in the stylesheet for the site (see above). -->
			<SitePoint:Editize id="BlurbEditize" runat="server"
				onContentChanged="FormContent_Changed" 
				width="100%" height="200px" paragraphstyles="false" paragraphalignments="false"
				bulletlists="false" numberedlists="false" hyperlinks="false" images="false"
				basefontface="Verdana" basefontsize="12" />
			<h3>Article:</h3>
			<!-- Here's our second instance of Editize. We leave all the features enabled
			  -- for this instance, and configure a 14 pixel font size, which matches the
			  -- stylesheet setting for the article text size (see above).
			  -- In addition, we provide an image list URL. -->
			<SitePoint:Editize id="ArticleEditize" runat="server"
				onContentChanged="FormContent_Changed" 
				width="100%" height="400px" basefontface="Verdana" basefontsize="14"
				linkurls="mailto:,http://www.sitepoint.com/article.php/"
				imglisturl="http://www.sitepoint.com/graphics/imglist.php" />
			<br />
			<asp:Button id="SubmitButton" runat="server" Text="Submit Article" />
		</form>
	</body>
</html>