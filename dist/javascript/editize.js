/**
 * A JavaScript API that allows developers to easily insert
 * the Editize form control into their HTML forms. See the
 * product documentation for details on the use of this
 * API, as comments in this file focus on implementation
 * of that API only.
 */
function Editize()
{
	// The name of the form element for the Editize control.
	// Normally the user will assign a name, but we assign a default
	// here just in case.
	this.name = "editize";
	
	// The codebase path. If set, this is the relative or absolute URL
	// of the directory that contains the Editize Applet JAR files
	// and license file(s).
	this.codebase = ".";
	this.licensefileext = "";
	this.ns4support = "";
	
	// The width of the Editize control
	this.width = 600;
	
	// The height of the Editize control.
	this.height = 600;
	
	// Show submit button as part of applet if true.
	this.showsubmitbutton = "";
	this.submitbuttonlabel = "";
	this.submitbuttonname = "";
	
	this.bgcolor = "";
	
	// Base font properties
	this.basefontface = "";
	this.basefontsize = "";
	this.basefontcolor = "";
        
	// Heading properties
	this.headfontface = "";
	this.headfontsize = "";
	this.headfontcolor = "";
	
	// Subheading properties
	this.subheadfontface = "";
	this.subheadfontsize = "";
	this.subheadfontcolor = "";
	
	// Block Quote properties
	this.insetfontface = "";
	this.insetfontsize = "";
	this.insetfontcolor = "";
	
	// Code block properties
	this.monospacedbackgroundcolor = "";
	
	// Highlighting properties
	this.highlightcolor = "";
	this.blockhighlightcolor = "";

	// Link properties
	this.linkcolor = "";
	
	// Enable/disable formatting features
	this.about = "";
	this.editbuttons = "";
	this.paragraphstyles = "";
	this.headingstyle = "";
	this.subheadingstyle = "";
	this.insetstyle = "";
	this.monospacedstyle = "";
	this.paragraphalignments = "";
	this.bulletlists = "";
	this.numberedlists = "";
	this.boldtext = "";
	this.italictext = "";
	this.underlinetext = "";
	this.highlighttext = "";
	this.highlightblock = "";
	this.inlinecode = "";
	this.hyperlinks = "";
	this.linkurls = new Array();
	this.tableclasses = new Array();
	this.tables = "";
	this.codeview = "";
	this.appletbgcolor = "";
	this.htmlclipboard = "";
	this.xhtmlstrict = "";
	this.formelementsallowed = "";

	// Image properties
	this.images = "";
	this.baseurl = "";
	this.imglisturl = "";
	
	// Register object methods
	this.display = Display;
	this.endForm = EndForm;
	this.displaySubmit = DisplaySubmit;
	this.trueOrFalse = TrueOrFalse;
	this.browserDetect = BrowserDetect;
	this.inAgent = InAgent;
	this.inArray = InArray;

	this.submitbuttonnames = new Array();
	
	return;
        
	/**
	 * Displays the Editize control using code suitable for whatever
	 * browser is detected. Should be called inside a form.
	 */
	function Display()
	{
		var browser = this.browserDetect();

		// Write out the <div> that will house the submission applet now
		// if NS4 support is not required. This would cause NS4 to end the form
		// prematurely, so we will output a standard <textarea> instead of
		// an Editize field in that browser.
		if (!this.ns4support && !__submitDivOutput && browser != 'nswin' && browser != 'nsunix' && browser != 'nsmac' && browser != 'iewin' && browser != 'ns6' && browser != 'ns6mac' && browser != 'safari') {
			document.writeln('<div id="editize_submit_div" style="position: absolute;"></div>');
			__submitDivOutput = true;
		}
		
		var content = "";
		if (arguments.length > 0) content = arguments[0];
		var contentForHTML = htmlspecialchars(content);
		var contentForJava = javaspecialchars(content);
		
		// Build associative array of attributes
		var attribs = new Object();
		attribs["fieldid"]							= this.name;
		if (this.codebase !== "")		attribs["codebase"]		= this.codebase;
		if (this.licensefileext !== "")		attribs["licenseext"]		= this.licensefileext;
		if (this.showsubmitbutton !== "")	attribs["showsubmitbutton"]	= this.trueOrFalse(this.showsubmitbutton);
		if (this.submitbuttonlabel !== "")	attribs["submitbuttonlabel"]	= this.submitbuttonlabel;
		if (this.submitbuttonname !== "")	attribs["submitbuttonname"]	= this.submitbuttonname;
		if (this.bgcolor !== "")		attribs["bgcolor"]		= this.bgcolor;
		if (this.basefontface !== "")		attribs["basefontface"]		= this.basefontface;
		if (this.basefontsize !== "")		attribs["basefontsize"]		= this.basefontsize;
		if (this.basefontcolor !== "")		attribs["basefontcolor"]	= this.basefontcolor;
		if (this.headfontface !== "")		attribs["headingfontface"]	= this.headfontface;
		if (this.headfontsize !== "")		attribs["headingfontsize"]	= this.headfontsize;
		if (this.headfontcolor !== "")		attribs["headingfontcolor"]	= this.headfontcolor;
		if (this.subheadfontface !== "")	attribs["subheadingfontface"]	= this.subheadfontface;
		if (this.subheadfontsize !== "")	attribs["subheadingfontsize"]	= this.subheadfontsize;
		if (this.subheadfontcolor !== "")	attribs["subheadingfontcolor"]	= this.subheadfontcolor;
		if (this.insetfontface !== "")		attribs["blockquotefontface"]	= this.insetfontface;
		if (this.insetfontsize !== "")		attribs["blockquotefontsize"]	= this.insetfontsize;
		if (this.insetfontcolor !== "")		attribs["blockquotefontcolor"]	= this.insetfontcolor;
		if (this.monospacedbackgroundcolor!=="")	attribs["codebackgroundcolor"]	= this.monospacedbackgroundcolor;
		if (this.highlightcolor !== "")		attribs["highlightcolor"]	= this.highlightcolor;
		if (this.blockhighlightcolor !== "")		attribs["blockhighlightcolor"]	= this.blockhighlightcolor;
		if (this.linkcolor !== "")		attribs["linkcolor"]		= this.linkcolor;
		if (this.baseurl !== "")		attribs["docbaseurl"]		= this.baseurl;
		if (this.imglisturl !== "")		attribs["imglisturl"]		= this.imglisturl;
		if (this.showsubmitbutton !== "")	attribs["showsubmitbutton"]	= this.trueOrFalse(this.showsubmitbutton);
		if (this.paragraphstyles !== "")	attribs["paragraphstyles"]	= this.trueOrFalse(this.paragraphstyles);
		if (this.headingstyle !== "")		attribs["headingstyle"]		= this.trueOrFalse(this.headingstyle);
		if (this.subheadingstyle !== "")	attribs["subheadingstyle"]	= this.trueOrFalse(this.subheadingstyle);
		if (this.insetstyle !== "")		attribs["blockquotestyle"]	= this.trueOrFalse(this.insetstyle);
		if (this.monospacedstyle !== "")	attribs["codeblockstyle"]	= this.trueOrFalse(this.monospacedstyle);
		if (this.paragraphalignments !== "")	attribs["paragraphalignments"]	= this.trueOrFalse(this.paragraphalignments);
		if (this.bulletlists !== "")		attribs["bulletlists"]		= this.trueOrFalse(this.bulletlists);
		if (this.numberedlists !== "")		attribs["numberedlists"]	= this.trueOrFalse(this.numberedlists);
		if (this.boldtext !== "")		attribs["boldtext"]		= this.trueOrFalse(this.boldtext);
		if (this.italictext !== "")		attribs["italictext"]		= this.trueOrFalse(this.italictext);
		if (this.underlinetext !== "")		attribs["underlinetext"]	= this.trueOrFalse(this.underlinetext);
		if (this.highlighttext !== "")		attribs["highlighttext"]	= this.trueOrFalse(this.highlighttext);
		if (this.highlightblock !== "")		attribs["highlightblock"]	= this.trueOrFalse(this.highlightblock);
		if (this.inlinecode !== "")		attribs["inlinecode"]		= this.trueOrFalse(this.inlinecode);
		if (this.hyperlinks !== "")		attribs["hyperlinks"]		= this.trueOrFalse(this.hyperlinks);
		if (this.images !== "")			attribs["images"]		= this.trueOrFalse(this.images);
		if (this.about !== "")			attribs["about"]		= this.trueOrFalse(this.about);
		if (this.editbuttons !== "")	attribs["editbuttons"]		= this.trueOrFalse(this.editbuttons);
		if (this.tables !== "")			attribs["tables"]		= this.trueOrFalse(this.tables);
		if (this.codeview !== "")		attribs["codeview"]		= this.trueOrFalse(this.codeview);
		if (this.appletbgcolor !== "")	attribs["appletbgcolor"]	= this.appletbgcolor;
		if (this.htmlclipboard !== "")		attribs["htmlclipboardimport"]	= this.trueOrFalse(this.htmlclipboard);
    if (this.xhtmlstrict !== "")		attribs["xhtmlstrict"]	= this.trueOrFalse(this.xhtmlstrict);
    if (this.formelementsallowed !== "")		attribs["formelementsallowed"]	= this.trueOrFalse(this.formelementsallowed);

		if (this.linkurls.length > 0)
		{
			attribs["linkurls"] = "" + this.linkurls.length;
			for (var i=1; i<=this.linkurls.length; i++)
			{
				attribs["linkurls."+i] = this.linkurls[i-1];
			}
		}
		if (this.tableclasses.length > 0)
		{
			attribs["tableclasses"] = "" + this.tableclasses.length;
			for (var i=1; i<=this.tableclasses.length; i++)
			{
				attribs["tableclasses."+i] = this.tableclasses[i-1];
			}
		}
		
		// Output the hidden form field if on a supported browser
		if ((browser != 'nswin' && browser != 'nsunix' && browser != 'nsmac') || this.ns4support)
		{
			document.writeln("<input type=\"hidden\" id=\""+this.name+"\" name=\""+this.name+"\" value=\""+contentForHTML+"\" />");
		}
		
		if (this.showsubmitbutton && this.submitbuttonname !== "")
		{
			document.writeln("<input type=\"hidden\" id=\""+this.submitbuttonname+"\" id=\""+this.submitbuttonname+"\" value=\"\" />");
		}
                
		// In MSIE we use the <object> tag to load the Sun Java plugin
		if (browser == 'iewin')
		{
			document.writeln('<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" id="'+this.name+'_applet" width="'+this.width+'" height="'+this.height+'"');
			document.writeln(' codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_3_1_02-win.cab#Version=1,3,1,2">');
			document.writeln('  <param name="code" value="com.editize.EditizeApplet" />');
			document.writeln('  <param name="archive" value="editize.jar" />');
			document.writeln('  <param name="type" value="application/x-java-applet;jpi-version=1.3.1_02" />');
			document.writeln('  <param name="scriptable" value="true" />');
			document.writeln('  <param name="mayscript" value="true" />');
			for (attrib in attribs)
			{
				document.writeln('  <param name="'+attrib+'" value="'+htmlspecialchars(attribs[attrib])+'" />');
			}
			document.writeln('</object>');
		}
		// In NS4, we use an <embed> tag.
		else if (browser == 'nswin' || browser == 'nsunix' || browser == 'nsmac')
		{
			if (this.ns4support) {
				attribs['ns4'] = 'true';
				attribs['articleText'] = contentForJava;
				document.writeln('<embed type="application/x-java-applet;version=1.3"');
				document.writeln(' pluginspage="http://java.sun.com/j2se/1.3/"');
				document.writeln(' name="'+this.name+'_applet"');
				document.writeln(' id="'+this.name+'_applet"');
				document.writeln(' code="com.editize.EditizeApplet"');
				document.writeln(' archive="editize.jar"');
				document.writeln(' width="'+this.width+'"');
				document.writeln(' height="'+this.height+'"');
				document.writeln(' scriptable="true"');
				document.writeln(' mayscript="true"');
				for (attrib in attribs)
					document.writeln('  '+attrib+'="'+htmlspecialchars(attribs[attrib])+'"');
				document.writeln(' pluginspage="http://java.sun.com/j2se/1.3/"');
				document.writeln('></embed>');
			}
			else // NS4 support disabled. Output a <textarea> for this browser instead
			{
				document.writeln('<textarea name="' + this.name + '" id="' + this.name + '" rows="10" cols="40">');
				document.writeln(contentForHTML + '</textarea>');
			}
		}
		// In all other browsers, we assume Java2 <applet> tag support
		else
		{
			// Lets the applet notify Opera users that Java 1.4+ is required.
			if (browser.substring(0,5) == 'opera') attribs["opera"] = "true";
			if (browser == 'opera6') attribs["opera6"] = "true";
                        // Instructs the applet to use an alternate submission method in OS X.
			if (browser == 'ie5mac')
			{
				attribs['osx'] = 'true';
				attribs['articleText'] = contentForJava;
			}
			document.writeln('<applet code="com.editize.EditizeApplet" codebase="'+this.codebase+'" id="'+this.name+'_applet" archive="editize.jar" width="'+this.width+'" height="'+this.height+'" mayscript="true" scriptable="true">');
			for (attrib in attribs)
			{
				if (attrib == 'codebase') continue;
				document.writeln('  <param name="'+attrib+'" value="'+htmlspecialchars(attribs[attrib])+'" />');
			}
			document.writeln('</applet>');

			// Provides iframe for alternate submission method support in OS X
			if (attribs['osx'] == 'true') {
				document.writeln('<iframe name="'+this.name+'_submitframe" width="0" height="0" style="display:none;"></iframe>');
			}
		}

		if ((browser != 'nswin' && browser != 'nsunix' && browser != 'nsmac') || this.ns4support)
		{
			var editizeAppletId = this.name+'_applet';
			var editizeForm = __getObj(this.name).form;
			// Detect if the form has an onsubmit event handler, and if so grab it so
			// we can run it ourselves before running our own submit method.
			var submitHandler = null;
			if (editizeForm.onsubmit != null && editizeForm.onsubmit != __submitEditize)
				submitHandler = editizeForm.onsubmit;
			__editizeArray[__editizeArray.length] = new Array(editizeAppletId, submitHandler);
			editizeForm.onsubmit = __submitEditize;
			editizeForm.__editizeID = this.name;
			// Detect if JavaScript can tell Editize fields to submit directly
			if (browser == 'iewin' || browser == 'ns6' || browser == 'ns6mac' || browser == 'safari')
				__editizeJsToJava = true;
		}

		// Save a global variable for __submitEditize to use
		__editizeCodebase = this.codebase;
	}

	// Improves browser compatibility when called after closing the form
	function EndForm()
	{
		// Write out the <div> that will house the submission applet now
		// if NS4 support is required.
		if (this.ns4support && !__submitDivOutput) {
			document.writeln('<div id="editize_submit_div" style="position: absolute;"></div>');
			__submitDivOutput = true;
		}

		var editizeForm = __getObj(this.name).form;
		for (var i = 0; i < editizeForm.elements.length; i++)
		{
			if (editizeForm.elements[i].type == "submit" &&
				editizeForm.elements[i].onclick != __submitButtonEditize)
			{
				editizeForm.elements[i].editizeOnclick =
					editizeForm.elements[i].onclick != null ?
					editizeForm.elements[i].onclick : null;
				editizeForm.elements[i].onclick = __submitButtonEditize;
			}
		}
	}

	function DisplaySubmit()
	{
		var text = "";
		var width = 100;
		var height = 30;
		var name = "";
		
		if (arguments.length > 0) text = arguments[0];
		if (arguments.length > 1) width = arguments[1];
		if (arguments.length > 2) height = arguments[2];
		if (arguments.length > 3) name = arguments[3];
		
		var browser = this.browserDetect();

		if (name !== "" && !this.inArray(name, this.submitbuttonnames))
		{
			this.submitbuttonnames[this.submitbuttonnames.length] = name;
			document.writeln('<input type="hidden" name="'+name+'" id="'+name+'" value="" />');
		}

		if (browser == 'iewin')
		{
			document.writeln('<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" width="'+width+'" height="'+height+'"');
			document.writeln('  codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_3_1_02-win.cab#Version=1,3,1,2">');
			document.writeln('    <param name="codebase" value="'+this.codebase+'" />');
			document.writeln('    <param name="archive" value="editize.jar" />');
			document.writeln('    <param name="code" value="com.editize.EditizeSubmitter" />');
			document.writeln('    <param name="type" value="application/x-java-applet;jpi-version=1.3.1_02" />');
			document.writeln('    <param name="mayscript" value="true" />');
			document.writeln('    <param name="submitbuttonlabel" value="'+htmlspecialchars(text)+'" />');
			if (name !== "") document.writeln('    <param name="submitbuttonname" value="'+name+'" />');
			document.writeln('    <param name="fieldid" value="'+this.name+'" />');
			document.writeln('</object>');
		}
		else if (browser == 'nswin' || browser == 'nsunix' || browser == 'nsmac')
		{
			document.writeln('<embed type="application/x-java-applet;version=1.3"');
			document.writeln('  pluginspage="http://java.sun.com/j2se/1.3/"');
			document.writeln('  codebase="'+this.codebase+'"');
			document.writeln('  archive="editize.jar"');
			document.writeln('  code="com.editize.EditizeSubmitter"');
			document.writeln('  width="'+width+'"');
			document.writeln('  height="'+height+'"');
			document.writeln('  mayscript="true"');
			document.writeln('  pluginspage="http://java.sun.com/j2se/1.3/"');
			document.writeln('  submitbuttonlabel="'+htmlspecialchars(text)+'"');
			if (name !== "") document.writeln('  submitbuttonname="'+name+'"');
			document.writeln('  fieldid="'+this.name+'"></embed>');
		}
		else
		{
			document.writeln('<applet code="com.editize.EditizeSubmitter"');
			document.writeln('  codebase="'+this.codebase+'"');
			document.writeln('  archive="editize.jar"');
			document.writeln('  width="'+width+'" height="'+height+'"');
			document.writeln('  mayscript="true">');
			document.writeln('    <param name="submitbuttonlabel" value="'+htmlspecialchars(text)+'" />');
			if (name !== "") document.writeln('    <param name="submitbuttonname" value="'+name+'" />');
			document.writeln('    <param name="fieldid" value="'+this.name+'" />');
			document.writeln('    <param name="osx" value="'+this.trueOrFalse(browser == 'ie5mac')+'" />');
			document.writeln('</applet>');
		}
	}

	/**
	 * Converts whitespace characters no longer supported by
	 * JRE 1.3.1_01a or later to character codes that Editize
	 * will understand.
	 */
	function javaspecialchars(text)
	{
		var newText, c, l;
		l = text.length;
		newText = "";
		for (var i=0; i<l; i++)
		{
			switch (c = text.charAt(i))
			{
				case '\n':
					newText += '\\n';
					break;
				case '\r':
					newText += '\\r';
					break;
				case '\t':
					newText += '\\t';
					break;
				case '\\':
					newText += '\\\\';
					break;
				default:
					newText += c;
			}
		}
		return newText;
	}

	function htmlspecialchars(text)
	{
		var newText, c, l;
		l = text.length;
		newText = "";
		for (var i=0; i<l; i++)
		{
			switch (c = text.charAt(i))
			{
				case '<':
					newText += '&lt;';
					break;
				case '>':
					newText += '&gt;';
					break;
				case '&':
					newText += '&amp;';
					break;
				case '"':
					newText += '&quot;';
					break;
				default:
					newText += c;
			}
		}
		return newText;
	}
	
	/**
	 * Takes a boolean and returns a 'true' or 'false' string.
	 */
	function TrueOrFalse(param)
	{
		return param ? 'true' : 'false';
	}

	/**
	 * Browser detection code
	 */
	function BrowserDetect()
	{
		var browser = "unknown";
		if ( this.inAgent('Opera 6') || this.inAgent('Opera 5') )
		{
			browser = 'opera6';
		}
		else if ( this.inAgent('Opera') )
		{
			browser = 'opera';
		}
		else if ( this.inAgent('Safari') )
		{
			browser = 'safari';
		}
		else if ( this.inAgent('MSIE') )
		{
			if ( this.inAgent('Mac') )
				browser = this.inAgent('MSIE 5') ? 'ie5mac' : 'ie4mac';
			else if ( this.inAgent('Win') )
				browser = 'iewin';
		}
		else
		{
			if ( this.inAgent('Mozilla/5') || this.inAgent('Mozilla/6') )
			{
				if ( this.inAgent('Mac OS X') ) browser = 'ns6mac';
				else browser = 'ns6';
			}
			else if ( this.inAgent('Mozilla/4') )
			{
				if ( this.inAgent('Mac') ) browser = 'nsmac';
				else if ( this.inAgent('Win') ) browser = 'nswin';
				else browser = 'nsunix';
			}
		}
		return browser;
	}

	/**
	 * Utility function used by browserDetect().
	 */
	function InAgent(agent) {
		return navigator.userAgent.indexOf(agent) >= 0;
	}

	function InArray(needle, haystack) {
		for (i = haystack.length - 1; i >= 0; i--) {
			if (haystack[i] == needle) return true;
		}
		return false;
	}

}

/**
 * Editize support functions
 */
var __editizeArray = new Array();
var __submitDivOutput = false;
var __editizeJsToJava = false;
var __editizeCodebase = '';
var __editizeSubmitButton = null;
var __editizeSubmitOK = false;
function __getObj(id)
{
	if (document.getElementById) { // DOM-compliant browsers (MSIE5+, NSN6+, O5+)
		return document.getElementById(id);
	} else if (document.all) { // MSIE4
		return document.all[id];
	} else { // NSN4
		for (var i=0;i<document.forms.length;i++)
		{
			if (document.forms[i].elements[id])
				return document.forms[i].elements[id];
		}
		return eval("document."+id); // If all else fails...
	}
}
function __submitButtonEditize(event)
{
	// Record the last submit button clicked
	__editizeSubmitButton = this;
	return this.editizeOnclick != null ? this.editizeOnclick(event) : true;
}
function __submitEditize()
{
	// Tell Editize to submit if in a browser that supports
	// JavaScript to Java communication, then submit the form if
	// the original onsubmit event handler gives its blessing
	if (__editizeJsToJava) {
		// Tell each Editize field to submit if in a browser
		for (var i=0;i<__editizeArray.length;i++)
		{
			document.applets[__editizeArray[i][0]].writeToField();
			// Run any custom obsubmit event handler that was found for the form
			if (__editizeArray[i][1] != null)
			{
				var retval = __editizeArray[i][1]();
				if (retval === false) return false;
			}
		}
		__editizeSubmitButton = null;
		return true;
	}
	// Load the applet that triggers submission in browsers that don't support
	// JavaScript-to-Java communication, then block submission, which will be
	// triggered by the __submitCallback(id) callback function.
	else if (navigator.userAgent.indexOf('MSIE') < 0 && 
			navigator.userAgent.indexOf('Opera') < 0 && 
			navigator.userAgent.indexOf('safari') < 0 && 
			navigator.userAgent.indexOf('Mozilla/4') >= 0) {
		if (__editizeSubmitOK)
		{
			__editizeSubmitOK = false;
			return true;
		}
		var submitDiv = document.layers['editize_submit_div'];
		var appletHtml = '';
		appletHtml += '<embed type="application/x-java-applet;version=1.3"\n';
		appletHtml += '  pluginspage="http://java.sun.com/j2se/1.3/"\n';
		appletHtml += '  codebase="' + __editizeCodebase + '"\n';
		appletHtml += '  archive="editize.jar"\n';
		appletHtml += '  code="com.editize.EditizeSubmitter"\n';
		appletHtml += '  width="1" height="1"\n';
		appletHtml += '  mayscript="true"\n';
		appletHtml += '  pluginspage="http://java.sun.com/j2se/1.3/"\n';
		appletHtml += '  fieldid="' + this.__editizeID + '"\n';
		appletHtml += '  immediate="true"></embed>';

		submitDiv.document.open();
		submitDiv.document.write(appletHtml);
		submitDiv.document.close();

		return false; // Wait for callback before submitting
	}
	// Same goes for IE Mac, and other unidentified browsers
	else
	{
		if (__editizeSubmitOK)
		{
			__editizeSubmitOK = false;
			return true;
		}
		var submitDiv = document.getElementById('editize_submit_div');
		var appletHtml = '';
		appletHtml += '<applet code="com.editize.EditizeSubmitter"\n';
		appletHtml += '  codebase="' + __editizeCodebase + '"\n';
		appletHtml += '  archive="editize.jar"\n';
		appletHtml += '  width="1" height="1"\n';
		appletHtml += '  mayscript="true">\n';
		appletHtml += '    <param name="fieldid" value="' + this.__editizeID + '" />\n';
		appletHtml += '    <param name="immediate" value="true" />\n';
		var osx = navigator.userAgent.indexOf('MSIE 5') >= 0 && navigator.userAgent.indexOf('Mac') >= 0;			
		appletHtml += '    <param name="osx" value="' + (osx ? 'true' : 'false') + '" />\n';
		appletHtml += '</applet>\\n';

		submitDiv.innerHTML = appletHtml;

		return false; // Wait for callback before submitting
	}
}
function __ns4submit(field, value)
{
	__getObj(field).value += value;
}
function __submitCallback(id)
{
	// Submit the form if the original onsubmit event handler gives its blessing
	for (var i=0;i<__editizeArray.length;i++)
	{
		if (__editizeArray[i][1] != null)
		{
			var retval = __editizeArray[i][1]();
			if (retval === false) return false;
		}
	}
	if (__editizeSubmitButton != null)
	{
		__editizeSubmitOK = true;
		__editizeSubmitButton.click();
		__editizeSubmitButton = null;
	}
	else
	{
		__getObj(id).form.submit();
	}
}
