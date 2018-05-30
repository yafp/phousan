# phousan [![License](https://img.shields.io/badge/license-GPL3-brightgreen.svg)](LICENSE) [![Build Status](https://travis-ci.org/yafp/phousan.svg?branch=master)](https://travis-ci.org/yafp/phousan) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/30952d1d10d34eb2a1ee3c32704c91b4)](https://www.codacy.com/app/yafp/phousan?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yafp/phousan&amp;utm_campaign=Badge_Grade) <a href="https://github.com/yafp/phousan/releases/latest" target="_blank"><img src="https://img.shields.io/github/release/yafp/phousan.svg" alt="Release"></a>


![logo](https://raw.githubusercontent.com/yafp/phousan/master/doc/Misc/app_icon_96.png)

A display-on event monitor for Android


## About

Phousan is a simple phone usage analyzer. It counts each display-on event and stores your usage history on a daily basis.

Get aware how addicted you are.


__Implementation:__

Phousan comes with a background service which is using a receiver to catch specific system events.

It is listening for [ACTION_SCREEN_ON](https://developer.android.com/reference/android/content/Intent#action_screen_on)
 to count the amount of display activations per day.


## Download
* ADD GOOGLE PLAY LINK


## Features
- counts display-on events per day

##### additional
- store max usage count (with date)
- store min usage count (with date)
- calculate average usage count
- write usage per day history
- export usage per day history
- delete all data (app reset)
- recommend app function


## Requirements
- Android 5.0 or higher


## Translations
* English
* German


## User-Interface

<img src="https://raw.githubusercontent.com/yafp/phousan/master/doc/Misc/ui_1_main_en.png" width="300">

<img src="https://raw.githubusercontent.com/yafp/phousan/master/doc/Misc/ui_2_menu_en.png" width="300">

<img src="https://raw.githubusercontent.com/yafp/phousan/master/doc/Misc/ui_3_history_en.png" width="300">


## Credits
* [CONTRIBUTING.md](CONTRIBUTING.md) for general instructions
* [CONTRIBUTORS.md](CONTRIBUTORS.md) for a lists of all contributors.
* [RESOURCES.md](RESOURCES.md) for a lists of all used resources.
