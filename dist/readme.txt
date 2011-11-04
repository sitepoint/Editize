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

2 Where's the Manual?
---------------------

Product documentation is in HTML format and may be found in in the 'manual'
directory of this distribution. Open manual/index.html in a Web browser to
view it.

The history of changes in this release may be found manual/history.html.

3 Known Problems
----------------

The following known problems exist in this version of Editize. We believe
they are all relatively minor. They will be corrected, where possible, in a
future release.

 - Occasionally when inserting a hyperlink in Safari, the browser will
   hang. We are pursuing a fix for this issue.

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
