# phousan Changelog

## phousan 0.4.0 (2018xxyy)
### ```Added```
* Added some firebase events to updateUI

### ```Fixed```
* Fixed translation error in german strings.



## phousan 0.3.0 (20180529)
### ```Added```
* Backup: Added a backupAgent for shared preferences (Issue #2)
* Service: Added a real background service (Issue #4)
* Service: starts on boot and on app usage if needed
* Menu: added start and stop entries for the new background service
* Menu: added visit GooglePlay entry

### ```Removed```
* UI: Removed percentage-diff-caculation between today and previous usage count

### ```Fixed```
* UI: Optimized UI for devices <= 5" (Issue #1)
* Leaving app with back-button resulted in counting errors (Issue #3)
* Replaced all deprecated calls



## phousan 0.2.0 (20180518)
### ```Added```
* __About__ window
* Option to delete all usage data
* Added percentage diff between todays- and yesterdays-usage count
* Added export function for usage data
* Added support for notifications

### ```Changed```
* Added __popupmenu__ (replacing several image buttons)
* Output of __Usage History per day__ is now sorted (reverse order)
* Enhanced code quality (based on Codacy analyze)
* New app icon

### ```Fixed```
* History data is now displayed
* UI is now refreshed after deleting usage data
* Fixed a wrong +1 on app relaunch without real event in between
* Fixed unescaped character in string



## phousan 0.1.0 (20180508)
### ```Added```
* First release
* Count display on events per day
* Write history (events per day, low- and highscore)
* Recommend app function
* Localization: english and german



## Info
### Versioning

```
MAJOR.MINOR.PATCH
```

* ```MAJOR``` version (incompatible API changes,)
* ```MINOR``` version (adding functionality)
* ```PATCH``` version (bug fixes)


### Categories
* ```Added```: for new features
* ```Changed```: for changes in existing functionality.
* ```Deprecated```: for soon-to-be removed features.
* ```Removed```: for now removed features.
* ```Fixed```: for any bug fixes.
* ```Security```: in case of vulnerabilities.
