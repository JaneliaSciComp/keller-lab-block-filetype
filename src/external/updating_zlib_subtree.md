zlib is a subtree from from https://github.com/madler/zlib at cacf7f1 (v1.2.11), as fetched on January 22nd, 2021 by Mark Kittisopikul

To update:
```
git remote add -f zlib https://github.com/madler/zlib.git
git pull -s subtree zlib master
```

The subtree merge was created by the following commands:
```
git remote add -f zlib https://github.com/madler/zlib.git
git merge -s ours --no-commit --allow-unrelated-histories zlib/master
git read-tree --prefix=src/external/zlib -u zlib/master
git commit
```
