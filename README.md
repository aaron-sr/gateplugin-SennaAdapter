gateplugin-SennaAdapter
========================

A plugin for the GATE language technology framework that provides a processing resource to integrate SENNA v3.0 (https://ronan.collobert.com/senna/)

Current Features
----------------
* reusable java wrapper to execute SENNA and parse STDOUT, all command-line parameters are supported
* multiprocessor architecture by splitting documents and run several SENNA instances in parallel
* support predefined sentence and token annotations
* support GATE 8.5.1 plugin architecture

Hint: It does not bundle SENNA, as it has its own license (see https://ronan.collobert.com/senna/license.html).