bzip2 is from git://sourceware.org/git/bzip2.git at 8ca1faa3 (> 1.0.8) as fetched on January 23rd, 2021 by Mark Kittisopikul

To update:
```
git remote add -f bzip2 git://sourceware.org/git/bzip2.git
git pull -s subtree bzip2 master
```

The subtree merge was created by the following commands:
```
git remote add -f bzip2 git://sourceware.org/git/bzip2.git
git merge -s ours --no-commit --allow-unrelated-histories bzip2/master
git mv bzip2-1.0.6 bzip2
git commit
```
