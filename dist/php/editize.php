<?php

/**
 * A PHP API that allows developers to easily insert the
 * Editize form control into their HTML forms. See the
 * product documentation for details on the use of this
 * API, as comments in this file focus on implementation
 * of that API only.
 */
class Editize
{
  // The name of the form element for the Editize control.
  // Normally the user will assign a name, but we assign a default
  // here just in case.
  var $name = "editize";

  // The codebase path. If set, this is the relative or absolute URL
  // of the directory that contains the Editize Applet JAR files.
  var $codebase = ".";
  var $ns4support = FALSE;

  // The width of the Editize control
  var $width = 600;

  // The height of the Editize control.
  var $height = 600;

  // Show submit button as part of applet if true.
  var $showsubmitbutton = FALSE;
  var $submitbuttonlabel;
  var $submitbuttonname;

  var $bgcolor;

  // Base font properties
  var $basefontface;
  var $basefontsize;
  var $basefontcolor;

  // Heading properties
  var $headfontface;
  var $headfontsize;
  var $headfontcolor;

  // Subheading properties
  var $subheadfontface;
  var $subheadfontsize;
  var $subheadfontcolor;

  // Block Quote properties
  var $insetfontface;
  var $insetfontsize;
  var $insetfontcolor;

  // Code block properties
  var $monospacedbackgroundcolor;

  // Highlighting properties
  var $highlightcolor;
  var $blockhighlightcolor;

  // Link properties
  var $linkcolor;

  // Image properties
  var $images;
  var $baseurl;
  var $imglisturl;

  // Enable/disable formatting features
  var $about;
  var $editbuttons;
  var $paragraphstyles;
  var $headingstyle;
  var $subheadingstyle;
  var $insetstyle;
  var $monospacedstyle;
  var $paragraphalignments;
  var $bulletlists;
  var $numberedlists;
  var $boldtext;
  var $italictext;
  var $underlinetext;
  var $highlighttext;
  var $highlightblock;
  var $inlinecode;
  var $hyperlinks;
  var $linkurls = array();
  var $tableclasses = array();
  var $tables;
  var $codeview;
  var $htmlclipboard;
  var $xhtmlstrict;
  var $formelementsallowed;
  var $appletbgcolor;

  var $cabfileurl = 'http://java.sun.com/products/plugin/autodl/jinstall-1_3_1_02-win.cab';

  /**
   * Displays the Editize control using code suitable for whatever
   * browser is detected. Should be called inside a form.
   */
  function display($content = '')
  {
    echo $this->getCode($content);
  }

  /**
   * Gets the code that must be output to display the Editize control
   * in the browser detected. Call this if you need to store the code
   * and output it at will (e.g. in a template system).
   */
  function getCode($content = '')
  {
    global $editizeFirstDisplayed;

    $output = '';

    $browser = $this->browserDetect();

    if (!$editizeFirstDisplayed)
    {
      // Output supporting JavaScript code to be shared betweeen multiple instances
      $output .= '
<!-- Editize support functions -->
<script language="JavaScript" type="text/javascript">
var __editizeArray = new Array();
var __editizeSubmitButton = null;
var __editizeSubmitOK = false;

// cross-browser object reference fetcher
function __getObj(id)
{
  if (document.getElementById) { // DOM-compliant browsers (MSIE5+, NSN6+, O5+)
    return document.getElementById(id);
  } else if (document.all) { // MSIE4
    return document.all[id];
  } else { // NSN4
    for (var i = 0; i < document.forms.length; i++)
    {
      if (document.forms[i].elements[id])
        return document.forms[i].elements[id];
    }
    return eval("document."+id); // If all else fails...
  }
}

// onclick event handler for submit buttons
function __submitButtonEditize(event)
{
  // Record the last submit button clicked
  __editizeSubmitButton = this;
  return this.editizeOnclick != null ? this.editizeOnclick(event) : true;
}

// onsubmit handler for forms containing Editize
function __submitEditize()
{';
      // Tell each Editize field to submit if in a browser that supports
      // JavaScript to Java communication, then submit the form if
      // the original onsubmit event handler gives its blessing
      if ($browser == 'iewin' or $browser == 'ns6' or $browser == 'ns6mac' or $browser == 'safari') {
        $output .= '
  for (var i = 0; i < __editizeArray.length; i++)
    {
    document.applets[__editizeArray[i][0]].writeToField();
    // Run any custom onsubmit handler that was found for the form
    if (__editizeArray[i][1] != null)
    {
      var retval = __editizeArray[i][1]();
      if (retval === false) return false;
    }
  }
  __editizeSubmitButton = null;
  return true;
}';
      }

      // Load the applet that triggers submission in browsers that don't support
      // JavaScript-to-Java communication, then block submission, which will be
      // triggered by the __submitCallback(id) callback function.
      // Also include __ns4submit function, used to bypass Netscape 4 crash on submit.
      elseif ($browser == 'nswin' or $browser == 'nsunix' or $browser == 'nsmac')
      {
        $output .= '
  if (__editizeSubmitOK)
  {
    __editizeSubmitOK = false;
    return true;
  }
  var submitDiv = document.layers["editize_submit_div"];
  var appletHtml = "";
  appletHtml += "<embed type=\\"application/x-java-applet;version=1.3\\"\\n";
  appletHtml += "  pluginspage=\\"http://java.sun.com/j2se/1.3/\\"\\n";
  appletHtml += "  codebase=\\"' . $this->codebase . '\\"\\n";
  appletHtml += "  archive=\\"editize.jar\\"\\n";
  appletHtml += "  code=\\"com.editize.EditizeSubmitter\\"\\n";
  appletHtml += "  width=\\"1\\" height=\\"1\\"\\n";
  appletHtml += "  mayscript=\\"true\\"\\n";
  appletHtml += "  pluginspage=\\"http://java.sun.com/j2se/1.3/\\"\\n";
  appletHtml += "  fieldid=\\"' . $this->name . '\\"\\n";
  appletHtml += "  immediate=\\"true\\"></embed>";

  submitDiv.document.open();
  submitDiv.document.write(appletHtml);
  submitDiv.document.close();

  return false; // Wait for callback before submitting
}

function __ns4submit(field, value)
{
  __getObj(field).value += value;
}';
      }

      // Same goes for IE 5 Mac, and other unidentified browsers.
      else
      {
        $output .= '
  if (__editizeSubmitOK)
  {
    __editizeSubmitOK = false;
    return true;
  }
  var submitDiv = document.getElementById("editize_submit_div");
  var appletHtml = "";
  appletHtml += "<applet code=\\"com.editize.EditizeSubmitter\\"\\n";
  appletHtml += "  codebase=\\"' . $this->codebase . '\\"\\n";
  appletHtml += "  archive=\\"editize.jar\\"\\n";
  appletHtml += "  width=\\"1\\" height=\\"1\\"\\n";
  appletHtml += "  mayscript=\\"true\\">\\n";
  appletHtml += "    <param name=\\"fieldid\\" value=\\"' . $this->name . '\\" />\\n";
  appletHtml += "    <param name=\\"immediate\\" value=\\"true\\" />\\n";
  appletHtml += "    <param name=\\"osx\\" value=\\"' . ($browser == 'ie5mac' ? 'true' : 'false') . '\\" />\\n";
    appletHtml += "</applet>\\n";

    submitDiv.innerHTML = appletHtml;

    return false; // Wait for callback before submitting
}';
      }

      $output .= '
// callback functions for applet-initiated form submissions
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
</script>
';

      // Write out the <div> that will house the submission applet now
      // if NS4 support is not required. This would cause NS4 to end the form
      // prematurely, so we will output a standard <textarea> instead of
      // an Editize field in that browser.
      if (!$this->ns4support && $browser != 'nswin' && $browser != 'nsunix' && $browser != 'nsmac' && $browser != 'iewin' && $browser != 'ns6' && $browser != 'ns6mac' && $browser != 'safari') {
        $output .= '
<div id="editize_submit_div" style="position: absolute;"></div>';
      }

      $editizeFirstDisplayed = TRUE;
    }

    $contentForHTML = htmlspecialchars($content);
    $contentForJava = $this->javaspecialchars($content);

    // Build an array of parameters
    $attribs = array();
    $attribs['fieldid'] = $this->name;
    if (isset($this->codebase)) $attribs['codebase'] = $this->codebase;
    if (isset($this->licensefileext)) $attribs['licenseext'] = $this->licensefileext;
    if (isset($this->showsubmitbutton)) $attribs['showsubmitbutton'] = $this->trueOrFalse($this->showsubmitbutton);
    if (isset($this->submitbuttonlabel)) $attribs['submitbuttonlabel'] = $this->submitbuttonlabel;
    if (isset($this->submitbuttonname)) $attribs['submitbuttonname'] = $this->submitbuttonname;
    if (isset($this->bgcolor)) $attribs['bgcolor'] = $this->bgcolor;
    if (isset($this->basefontface)) $attribs['basefontface'] = $this->basefontface;
    if (isset($this->basefontsize)) $attribs['basefontsize'] = $this->basefontsize;
    if (isset($this->basefontcolor)) $attribs['basefontcolor'] = $this->basefontcolor;
    if (isset($this->headfontface)) $attribs['headingfontface'] = $this->headfontface;
    if (isset($this->headfontsize)) $attribs['headingfontsize'] = $this->headfontsize;
    if (isset($this->headfontcolor)) $attribs['headingfontcolor'] = $this->headfontcolor;
    if (isset($this->subheadfontface)) $attribs['subheadingfontface'] = $this->subheadfontface;
    if (isset($this->subheadfontsize)) $attribs['subheadingfontsize'] = $this->subheadfontsize;
    if (isset($this->subheadfontcolor)) $attribs['subheadingfontcolor'] = $this->subheadfontcolor;
    if (isset($this->insetfontface)) $attribs['blockquotefontface'] = $this->insetfontface;
    if (isset($this->insetfontsize)) $attribs['blockquotefontsize'] = $this->insetfontsize;
    if (isset($this->insetfontcolor)) $attribs['blockquotefontcolor'] = $this->insetfontcolor;
    if (isset($this->monospacedbackgroundcolor)) $attribs['codebackgroundcolor'] = $this->monospacedbackgroundcolor;
    if (isset($this->highlightcolor)) $attribs['highlightcolor'] = $this->highlightcolor;
    if (isset($this->blockhighlightcolor)) $attribs['blockhighlightcolor'] = $this->blockhighlightcolor;
    if (isset($this->linkcolor)) $attribs['linkcolor'] = $this->linkcolor;
    if (isset($this->paragraphstyles)) $attribs['paragraphstyles'] = $this->trueOrFalse($this->paragraphstyles);
    if (isset($this->headingstyle)) $attribs['headingstyle'] = $this->trueOrFalse($this->headingstyle);
    if (isset($this->subheadingstyle)) $attribs['subheadingstyle'] = $this->trueOrFalse($this->subheadingstyle);
    if (isset($this->insetstyle)) $attribs['blockquotestyle'] = $this->trueOrFalse($this->insetstyle);
    if (isset($this->monospacedstyle)) $attribs['codeblockstyle'] = $this->trueOrFalse($this->monospacedstyle);
    if (isset($this->paragraphalignments)) $attribs['paragraphalignments'] = $this->trueOrFalse($this->paragraphalignments);
    if (isset($this->bulletlists)) $attribs['bulletlists'] = $this->trueOrFalse($this->bulletlists);
    if (isset($this->numberedlists)) $attribs['numberedlists'] = $this->trueOrFalse($this->numberedlists);
    if (isset($this->boldtext)) $attribs['boldtext'] = $this->trueOrFalse($this->boldtext);
    if (isset($this->italictext)) $attribs['italictext'] = $this->trueOrFalse($this->italictext);
    if (isset($this->underlinetext)) $attribs['underlinetext'] = $this->trueOrFalse($this->underlinetext);
    if (isset($this->highlighttext)) $attribs['highlighttext'] = $this->trueOrFalse($this->highlighttext);
    if (isset($this->highlightblock)) $attribs['highlightblock'] = $this->trueOrFalse($this->highlightblock);
    if (isset($this->inlinecode)) $attribs['inlinecode'] = $this->trueOrFalse($this->inlinecode);
    if (isset($this->hyperlinks)) $attribs['hyperlinks'] = $this->trueOrFalse($this->hyperlinks);
    if (isset($this->images)) $attribs['images'] = $this->trueOrFalse($this->images);
    if (isset($this->about)) $attribs['about'] = $this->trueOrFalse($this->about);
    if (isset($this->editbuttons)) $attribs['editbuttons'] = $this->trueOrFalse($this->editbuttons);
    if (isset($this->tables)) $attribs['tables'] = $this->trueOrFalse($this->tables);
    if (isset($this->codeview)) $attribs['codeview'] = $this->trueOrFalse($this->codeview);
    if (isset($this->htmlclipboard)) $attribs['htmlclipboardimport'] = $this->trueOrFalse($this->htmlclipboard);
    if (isset($this->xhtmlstrict)) $attribs['xhtmlstrict'] = $this->trueOrFalse($this->xhtmlstrict);
    if (isset($this->formelementsallowed)) $attribs['formelementsallowed'] = $this->trueOrFalse($this->formelementsallowed);
    if (isset($this->appletbgcolor)) $attribs['appletbgcolor'] = $this->appletbgcolor;
    if (isset($this->baseurl)) $attribs['docbaseurl'] = $this->baseurl;
    if (isset($this->imglisturl)) $attribs['imglisturl'] = $this->imglisturl;
    if (isset($this->linkurls))
    {
      $attribs['linkurls'] = count($this->linkurls);
      for ($i=1; $i<=count($this->linkurls); $i++)
      {
        $attribs['linkurls.'.$i] = $this->linkurls[$i-1];
      }
    }
    if (isset($this->tableclasses))
    {
      $attribs['tableclasses'] = count($this->tableclasses);
      for ($i=1; $i<=count($this->tableclasses); $i++)
      {
        $attribs['tableclasses.'.$i] = $this->tableclasses[$i-1];
      }
    }

    // Output the hidden form field if on a supported browser
    if (($browser != 'nswin' && $browser != 'nsunix' && $browser != 'nsmac') || $this->ns4support)
    {
      $output .= "<input type=\"hidden\" id=\"{$this->name}\" name=\"{$this->name}\" value=\"{$contentForHTML}\" />";
    }

    if ($attribs['showsubmitbutton'] && isset($attribs['submitbuttonname']))
    {
      $output .= '
<input type="hidden" name="'.htmlspecialchars($attribs['submitbuttonname']).'" id="'.htmlspecialchars($attribs['submitbuttonname']).'" value="" />';
    }

    // In MSIE we use the <object> tag to load the Sun Java plugin
    if ($browser == 'iewin'):

      $output .= '
<!-- Editize -->
<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" id="'.$this->name.'_applet" width="'.$this->width.'" height="'.$this->height.'"
  codebase="'.$this->cabfileurl.'#Version=1,3,1,2">
  <param name="code" value="com.editize.EditizeApplet" />
  <param name="archive" value="editize.jar" />
  <param name="type" value="application/x-java-applet;jpi-version=1.3.1_02" />
  <param name="scriptable" value="true" />
  <param name="mayscript" value="true" />
';
      foreach ($attribs as $key => $value)
      {
        $output .= "\t<param name=\"$key\" value=\"".htmlspecialchars($value)."\" />\n";
      }
      $output .= "</object>\n";

    // In NS4, we use an <embed> tag.
    elseif ($browser == 'nswin' or $browser == 'nsunix' or $browser == 'nsmac'):
      if ($this->ns4support)
      {
        $output .= '
<!-- Editize -->
<embed type="application/x-java-applet;version=1.3"
  pluginspage="http://java.sun.com/j2se/1.3/"
  name="'.$this->name.'_applet"
  id="'.$this->name.'_applet"
  code="com.editize.EditizeApplet"
  archive="editize.jar"
  width="'.$this->width.'"
  height="'.$this->height.'"
  scriptable="true"
  mayscript="true"
';

        $attribs['ns4'] = 'true';
        $attribs['articleText'] = $contentForJava;

        foreach ($attribs as $key => $value)
        {
          $output .= "\t$key=\"".htmlspecialchars($value)."\"\n";
        }

        $output .= '	pluginspage="http://java.sun.com/j2se/1.3/">
</embed>
';
      }
      else // NS4 support disabled. Output a <textarea> for this browser instead
      {
        $output .= '<textarea name="'.$this->name.'" id="'.$this->name.'" rows="10" cols="40">
'.$contentForHTML.'</textarea>';
      }

    // In all other browsers, we assume Java2 <applet> tag support
    else:
      // Lets the applet notify Opera users that Java 1.4+ is required.
      if (strpos($browser,'opera') !== FALSE) $attribs['opera'] = 'true';
      if ($browser == 'opera6') $attribs['opera6'] = 'true';
      // Instructs the applet to use an alternate submission method in OS X.
      if ($browser == 'ie5mac')
      {
        $attribs['osx'] = 'true';
        $attribs['articleText'] = $contentForJava;
      }

      $output .= '
<!-- Editize -->
<applet width="'.$this->width.'" height="'.$this->height.'" code="com.editize.EditizeApplet" codebase="'.$this->codebase.'" name="'.$this->name.'_applet" id="'.$this->name.'_applet" archive="editize.jar" mayscript="true" scriptable="true">
';
      foreach ($attribs as $key => $value)
      {
        if ($key == 'codebase') continue;
        $output .= "\t<param name=\"$key\" value=\"".htmlspecialchars($value)."\" />\n";
      }
      $output .= "</applet>\n";

      // Provides iframe for alternate submission method support in OS X.
      if (isset($attribs['osx']) && $attribs['osx'])
      {
        $output .= "\t<iframe name=\"".$this->name."_submitframe\" width=\"0\" height=\"0\" style=\"display:none;\"></iframe>\n";
      }

    endif;

    // Implant __submitEditize() onsubmit handler for the form if on a supported browser
    if (($browser != 'nswin' && $browser != 'nsunix' && $browser != 'nsmac') || $this->ns4support)
    {
      $output .= '
<script language="JavaScript" type="text/javascript">
var editizeAppletId = \''.$this->name.'_applet\';
var editizeForm = __getObj(\''.$this->name.'\').form;
var submitHandler = null;
if (editizeForm.onsubmit != null && editizeForm.onsubmit != __submitEditize)
  submitHandler = editizeForm.onsubmit;
__editizeArray[__editizeArray.length] = new Array(editizeAppletId, submitHandler);
editizeForm.onsubmit = __submitEditize;
</script>
<!-- End Editize -->
';
    }

    return $output;
  }

  // Improves browser compatibility when called
  function endForm()
  {
    echo $this->getEndFormCode();
  }

  function getEndFormCode()
  {
    $output = $this->ns4support ? '<div id="editize_submit_div" style="position: absolute;"></div>' : '';
    $output .= '
<script language="JavaScript" type="text/javascript">
var editizeForm = __getObj(\'' . $this->name . '\').form;
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
</script>';
    return $output;
  }

  function displaySubmit($text = '', $width = '', $height = '', $name = '')
  {
    echo $this->getSubmitCode($text, $width, $height, $name);
  }

  function getSubmitCode($text = '', $width = '', $height = '', $name = '')
  {
    static $submitbuttonnames = array();

    if ($text == '') $text = 'Submit';
    if ($width == '') $width = 100;
    if ($height == '') $height = 30;

    $browser = $this->browserDetect();

    $output = '';

    // One hidden field per name
    if ($name != '' and !in_array($name, $submitbuttonnames))
    {
      $submitbuttonnames[] = $name;
      $output .= '
<input type="hidden" name="'.htmlspecialchars($name).'" id="'.htmlspecialchars($name).'" value="" />';
    }

    if ($browser == 'iewin')
    {
      $output .= '
<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" width="'.$width.'" height="'.$height.'"
  codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_3_1_02-win.cab#Version=1,3,1,2">
  <param name="codebase" value="'.$this->codebase.'" />
  <param name="archive" value="editize.jar" />
  <param name="code" value="com.editize.EditizeSubmitter" />
  <param name="type" value="application/x-java-applet;jpi-version=1.3.1_02" />
  <param name="mayscript" value="true" />
  <param name="submitbuttonlabel" value="'.htmlspecialchars($text).'" />';
      if ($name != '') $output .= '
  <param name="submitbuttonname" value="'.htmlspecialchars($name).'" />';
      $output .= '
  <param name="fieldid" value="'.$this->name.'" />
</object>';
    }
    elseif ($browser == 'nswin' or $browser == 'nsunix' or $browser == 'nsmac')
    {
      $output .= '
<embed type="application/x-java-applet;version=1.3"
  pluginspage="http://java.sun.com/j2se/1.3/"
  codebase="'.$this->codebase.'"
  archive="editize.jar"
  code="com.editize.EditizeSubmitter"
  width="'.$width.'"
  height="'.$height.'"
  mayscript="true"
  pluginspage="http://java.sun.com/j2se/1.3/"
  submitbuttonlabel="'.htmlspecialchars($text).'"';
      if ($name != '') $output .= '
  submitbuttonname="'.htmlspecialchars($name).'"';
      $output .= '
  fieldid="'.$this->name.'">
</embed>';
    }
    else
    {
      $output .= '
<applet code="com.editize.EditizeSubmitter"
  codebase="'.$this->codebase.'"
  archive="editize.jar"
  width="'.$width.'" height="'.$height.'"
  mayscript="true">
  <param name="submitbuttonlabel" value="'.htmlspecialchars($text).'" />';
      if ($name != '') $output .= '
  <param name="submitbuttonname" value="'.htmlspecialchars($name).'" />';
      $output .= '
  <param name="fieldid" value="'.$this->name.'" />
  <param name="osx" value="'.$this->trueOrFalse($browser == 'ie5mac').'" />
</applet>';
    }

    return $output;
  }

  /**
   * Converts whitespace characters no longer supported by
   * JRE 1.3.1_01a or later to character codes that Editize
   * will understand.
   */
  function javaspecialchars($text)
  {
    return addcslashes($text, "\n\r\t\\");
  }

  /**
   * Takes a boolean and returns a 'true' or 'false' string.
   */
  function trueOrFalse($param)
  {
    return $param ? 'true' : 'false';
  }

  /**
   * Browser detection code
   */
  function browserDetect()
  {
    $browser = "unknown";
    if ( $this->inAgent('Opera 6') || $this->inAgent('Opera 5') )
    {
      $browser = 'opera6';
    }
    else if ( $this->inAgent('Opera') )
    {
      $browser = 'opera';
    }
    else if ( $this->inAgent('Safari') )
    {
      $browser = 'safari';
    }
    else if ( $this->inAgent('MSIE') )
    {
      if ( $this->inAgent('Mac') )
        $browser = $this->inAgent('MSIE 5') ? 'ie5mac' : 'ie4mac';
      elseif ( $this->inAgent('Win') )
        $browser = 'iewin';
    }
    else
    {
      if ( $this->inAgent('Mozilla/5') or $this->inAgent('Mozilla/6') )
      {
        if ( $this->inAgent('Mac OS X') ) $browser = 'ns6mac';
        else $browser = 'ns6';
      }
      elseif ( $this->inAgent('Mozilla/4') )
      {
        if ( $this->inAgent('Mac') ) $browser = 'nsmac';
        elseif ( $this->inAgent('Win') ) $browser = 'nswin';
        else $browser = 'nsunix';
      }
    }
    return $browser;
  }

  /**
   * Utility function used by browserDetect().
   */
  function inAgent($agent)
  {
    global $HTTP_SERVER_VARS;
    $notAgent = strpos($HTTP_SERVER_VARS['HTTP_USER_AGENT'],$agent) === false;
    return !$notAgent;
  }
}

/**
 * This is the equivalent of static initialization code for
 * the class above. It sets up the JavaScript upon which one or
 * more Editize instances shall rely.
 */
$editizeFirstDisplayed = FALSE;
?>
