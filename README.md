OC ReMix ReTagger
================

Retags default OC ReMix mp3 files, swapping the Title and Subtitle tags, and the Album and Grouping tags.

Also adds a Sort Album tag (TSOA) for Final Fantasy II - XIV Albums, and translates 'Nobuo Uematsu' in the Composer tag to Japanese (it's one of the few I can read).

### OCR's default ID3v2.3 tag format

| Tag | Format |
| --- | --- |
| Title (TIT2) | [Game] '[Remix Title]' OC ReMix |
| Subtitle (TIT3) | [Remix Title] |
| Album (TALB) | http://ocremix.org |
| Grouping (TIT1) | [Game] |

### Desired ID3v2.3 tag format

| Tag | Format |
| --- | --- |
| Title (TIT2) | [Remix Title] |
| Subtitle (TIT3) | [Game] '[Remix Title]' OC ReMix |
| Album (TALB) | [Game] |
| Grouping (TIT1) | http://ocremix.org |

## Usage

| Switches | Default | Desc |
| -------- | ------- | ----
| -h, --no-help, --help | false | Show help. |
| -p, --no-parse, --parse | false | Parse Tags. |
| -f, --no-files, --files | false | Process Files. |
| -d, --no-directory, --directory | false | Process Directories. |

#### Examples

Print the tags in one or more MP3 files:

    lein exec ocremix-retag.clj -p -f ~/Music/some_OC_ReMix.mp3 ~/Music/some_other_OC_ReMix.mp3

Print the tags in all MP3 files found under one or more directories:

    lein exec ocremix-retag.clj -p -d ~/Music/OC_ReMix_1-1000 ~/Music/OC_ReMix_1001-2000


Retag one or more MP3 files and save the results under a `retagged` directory, created along side the files:

    lein exec ocremix-retag.clj -f ~/Music/some_OC_ReMix.mp3 ~/Music/some_other_OC_ReMix.mp3

Retag all MP3 files in the `~/Music/OC_ReMix` directory and save the results under `~/Music/OC_ReMix/retagged`:

    lein exec ocremix-retag.clj -d ~/Music/OC_ReMix

## License

Copyright (C) 2014 Paul Sarando

Distributed under the Eclipse Public License, the same as Clojure.

