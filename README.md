# jdi-steprecorder

JDI Steprecorder is a tool that connects to a JVM via Java Debug Interface and records
execution of threads (including new threads) line by line into files with
corresponding thread names. This allows you to

1. Record multiple executions of your code (e.g. one in a configuration that causes a bug and one in a configuration that doesn't) and diff them
2. Study what the code is actually doing
3. Find good spots to put breakpoints for debugging in your IDE
4. Compare how some functionality behaves across different versions of your program. This should help to narrow down the search scope for regressions.
5. Calculate statistics to see which lines of code are executed most often
6. Finally see the time dimension of your program, that is usually implicit in imperative code.

## Installation

1. Find the [latest release](https://github.com/alesguzik/jdi-steprecorder/releases/latest) on github
2. Download `jdi-steprecorder`
3. Make it executable: `chmod +x jdi-steprecorder`
4. Move it somewhere on your PATH: `mv "$HOME/Downloads/jdi-steprecorder" ~/.local/bin/jdi-steprecorder`

## Usage

### Recording

1. Run your app with something like `-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005`.
2. Run `jdi-steprecorder` to start the recording.
3. Press [Enter] to stop the recording.

### Filtering out calls to the standard library

You may want to hide steps happening inside the standard library. This will make traces shorter
and easier to read and compare.

    $ cat main__4.trace | grep -v -E "^(java|sun)\." | uniq > main__4.nostdlib.trace

### Find lines that has been executed the most number of times.

    $ cat main__4.trace |sort|uniq -c|sort -nr|head -n 20

### Comparing recordings

Just make two recordings and use `diff` command or a gui diffing tool like [Meld](https://meldmerge.org/).

## Options

```
  Option               Default            Description
---------------------  -----------------  --------------------------------
  -p, --port PORT      5005               Port number
  -H, --host HOST      localhost          Hostname
  -d, --dir DIRECTORY  Current directory  Directory to place thread traces
  -h, --help                              Display help and exit
```

## TODO

- [ ] alternative output formats
- [ ] display the code of the lines
- [ ] Write `lldb-steprecorder` for working with C, C++, Rust and the like.

## Building from source

Install leiningen and run

    $ ./run build

...

## License

Copyright © 2019 Ales Huzik

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
