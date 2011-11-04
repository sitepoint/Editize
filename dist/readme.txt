EDITIZE 2.14 README
===================

CONTENTS

1 Introduction
2 Where's the Manual?
3 Known Problems

1 Introduction
--------------

Editize is designed as a drop-in replacement for the HTML <textarea> tag.
Instead of plain text, it allows your users to edit richly formatted
documents. When the form is submitted, Editize fields are submitted as
simple HTML documents. Like <textarea>, Editize fields can also be passed an
initial document to display when the page loads.

This distribution will allow you to install Editize on your local machine
for evaluation purposes. If you want to use Editize on a Web site, or on
some other machine on your network, you'll also need to obtain a FREE trial
license file from our Web site.

For more information about Editize in general, to obtain a FREE trial
license, or to see a demonstration of it in action, please visit us at:

  http://editize.com/

Some other URLs that may be of use:

  Obtain a Demo license to trial Editize on your Web site:
    http://demo.editize.com/

  Order Editize:
    http://register.editize.com/

  Manage your license files:
    http://login.editize.com/

2 Where's the Manual?
---------------------

Product documentation is in HTML format and may be found in in the 'manual'
directory of this distribution. Open manual/index.html in a Web browser to
view it.

Licensing information may be found in manual/license.html.

The history of changes in this release may be found manual/history.html.

3 Known Problems
----------------

The following known problems exist in this version of Editize. We believe
they are all relatively minor. They will be corrected, where possible, in a
future release.

If any of these issues affects the usability of Editize in your application,
please notify our support staff so that a fix can be assigned greater
priority.

 - Occasionally when inserting a hyperlink in Safari, the browser will
   hang. We are pursuing a fix for this issue.

 - Editize may not be able to download its license file through recent
   versions of Microsoft Proxy Server (including Microsoft ISA Server),
   as this server uses a non-standard authentication mechanism that Java
   versions prior to 1.4.2 do not support. To correct the problem,
   upgrade the client machine to Java (J2SE) 1.4.2 or later by visiting
   http://www.java.com/. Getting version 1.4.2_02 or later will ensure
   that the user experience will be transparent; earlier versions prompt
   the user for their network credentials in each browser session.

 - After the first use of Editize in a given browser session, Editize (and
   indeed all Java applets with system clipboard access) may become
   "detached" from the system clipboard and unable to paste in content
   from external programs. This is due to a bug in releases of Java prior
   to version 1.4.2. To correct the problem, upgrade the client machine to
   Java (J2SE) 1.4.2 or later by visiting http://www.java.com/.

 - We have identified a bug in Mozilla for Mac OS X that can prevent a
   Java applet like Editize from getting all its parameters from the
   browser. This can cause Editize to display "Editize could not access
   the form element", or ignore some of your configuration options when
   it first loads. We have found that pressing the Back button and then
   the Forward button (not the Refresh button!) will allow Editize to
   load correctly in such cases. We are pursuing a fix to this issue with
   the Mozilla development team.

 - The popup menu behaves strangely when multiple instances of Editize
   are open on the same page. To reproduce, right-click in one instance,
   then another, then click in the second instance. The popup should
   go away, but it doesn't until you've clicked at least once in the first
   instance.

 - With no selection, click Bold. The button reflects the new state. Move
   the cursor. The button no longer reflects the state, even though typing
   will still produce bold text now. Java maintains 'cursor-only' attributes
   even when the caret moves, even though the button states do not.

                                - THE END -
