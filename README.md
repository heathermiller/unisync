## uniSync is a super simple uni-directional sync <br>(written in Scala)

Why I needed uniSync:

I'm using [Jekyll](http://jekyllrb.com/) with github post-commit hooks to trigger a build of my website on the server, whenever I make a github commit. I wanted my Jekyll-generated site to be automatically merged with what already existed in my `public_html` folder on my web server. It sounds simple, but for some reason it's not. Why?:

- If you make `public_html` your Jekyll target directory, Jekyll will delete it and then regenerate the new site into it. Ouch.
- If you try to use rsync, you can definitely uni-directionally sync your Jekyll-generated site. But if you delete a bunch of stuff, or move some stuff into a new directory, rsync will just leave it there. For more on why rsync won't work, see the [SO Question](http://unix.stackexchange.com/questions/76739/rsync-delete-files-on-receiving-side-that-were-deleted-on-sending-side-but-do).
- Newer versions of `unison` have this neat `-nocreation` option which is supposed to prevent files from being created on the destination side. It had issues though- it still somehow bi-directionally synced two directories (meaning it _did_ "create" files on both sides, even though `-nocreation` should stop it.)
- Rumor has it that [`rdiff-backup` might work](http://unix.stackexchange.com/a/76769/39678). But after spending hours trying to get it to run on a server on which I wasn't an admin, I gave up.

## What it does

If you have a directory structure that looks like this:

     ── A
        ├── 1.scala
        ├── 2.scala
        └── 3.scala

     ── public_html
        ├── 3.scala
        ├── yahoo.wee
        └── yeehaw.scala


Let's say that whenever anything new arrives, or gets deleted, in directory `A`, you want those changes to be propagated to directory `public_html`. But we definitely don't want to damage anything else that doesn't exist in `A` but which exists in `public_html`.

Running `uniSync` results in this:

    ── A
       ├── 1.scala
       ├── 2.scala
       └── 3.scala

    ── public_html
       ├── 1.scala
       ├── 2.scala
       ├── 3.scala
       ├── yahoo.wee
       └── yeehaw.scala

#### You can delete stuff in the source directory too

Let's say we delete stuff in `A`, like: `2.scala` and `3.scala`. Running `uniSync` will result in this:

    ── A
       └── 3.scala

    ── public_html
        ├── 3.scala
        ├── yahoo.wee
        └── yeehaw.scala

## You want to use it?

Great. All you need is Java (1.6). Just [download the bundle (an executable script & three jars)](https://github.com/heathermiller/unisync/blob/master/unisync.zip?raw=true), unarchive them to a nice directory somewhere that's on your path, `chmod +x` your stuff, and that's it.

**Always call uniSync from the same directory**. Why? It writes a hidden file, called `.unisync` to the directory you called uniSync from. This file keeps track of what the state of your directory was the last time you called uniSync. It's used to figure out how your directory has changed since the last time you called uniSync (and what files you might've deleted).

**Example setup:**

For example, you might want to unarchive [the bundle](https://github.com/heathermiller/unisync/blob/master/unisync.zip?raw=true) to `~/bin` on your web server. Then, make sure that unisync is executable: `chmod +x unisync`.

**Example usage:**

    unisync ~/test/a ~/test/b

Before:

    $ tree test/
    test/
    ├── a
    │   ├── 1.txt
    │   ├── 2.txt
    │   └── 3.txt
    └── b
        ├── 3.txt
        ├── nono.txt
        └── yay.txt

After:

    $ tree test/
    test/
    ├── a
    │   ├── 1.txt
    │   ├── 2.txt
    │   └── 3.txt
    └── b
        ├── 1.txt
        ├── 2.txt
        ├── 3.txt
        ├── nono.txt
        └── yay.txt

## You want to add stuff to it?

Great. Just make a PR.

And of course: Unless required by applicable law or agreed to in writing, software
is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.