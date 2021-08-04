# qmetry-test-management-plugin

https://wiki.jenkins.io/display/JENKINS/QMetry+Test+Managment+Plugin

QMetry Test Management plugin for Jenkins has been designed to seamlessly integrate your CI/CD pipeline with QMetry.

Easily configure Jenkins to submit your test results to QMetry without needing to write any code or deal with REST API. 
Your Test Results could be from any automation framework like Cucumber, Test NG, JUnit, QAF or HP-UFT.

## Installing the plugin
Follow the steps to install QMetry Test Management Plugin directly from Jenkins marketplace.

1. Login to your Jenkins instance.
2. Go to *Manage Jenkins > Manage Plugins > Available*
3. Now search for **QMetry Test Management Plugin** and click *Install*

Alternatively, to create a new build, clone this repository and use the maven command
```
mvn build
```
Now install the **.hpi** file generated inside the */target* directory of the build into Jenkins. Follow the steps.

1. Login to your Jenkins instance.
2. Go to *Manage Jenkins > Manage Plugins > Advanced*
3. Under **Upload Plugin**, upload the hpi file generated.

## Using the plugin
We assume that you have a Jenkins project that produces test result files on build, using automation frameworks like Cucumber, TestNG, JUnit, HP-UFT, QAS. 
To upload result files to QMetry, follow the steps.

1. Login to Jenkins instance.
2. Go to your project > configure.
3. Under *Post Build Actions*, click on **Publish Build Result(s) to QMetry Test Management**.

Fill the QMetry configuration form as per your requirement.

* **QMetry instance URL** - url to your qtm instance
* **Automation API Key** - Automation Key
* **Automation Framework** - JUNIT/TESTNG/CUCUMBER/QAS/HP-UFT/ROBOT/JSON
* **Automation Hierarchy** - Hierarchy which will be used to parse test result files on QTM
* **Result File(s) Path/Directory** - path to result file (or directory for multiple files) relative to build directory
* **Project** - Name, ID or Key of QMetry Project
* **Test Suite ID** (optional) - Key/ID of test suite to be reused
* **Test Suite Name** (optional) - Test Suite Name (This will create a new test suite with given name)
* **Release** (optional if cycle not present) - Target Release ID or Release name
* **Cycle** (optional) - Target Cycle Id or Cycle Name
* **Build** (optional) - Target Build ID or Build name
* **Platform** (optional) - Name of the platform to connect the suite
* **Test Case Fields** (optional) - Test Case Fields in Json Format
* **Test Suite Fields** (optional) - Test Suite Fields in Json Format

Build your project from Jenkins and your test results should be automatically linked to the Test Suite specified, (or new Test Suite is created)

In Case of failure, check for following points :

* **Test Suite** should include *Test Suite Key or Id* from your QMetry Test Management project. Ignore the field if you want to create a new Test Suite for the results.
* **Platform** (if specified), must be included in your QMetry Test Management project, before the task is executed.