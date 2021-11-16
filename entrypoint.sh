#!/bin/sh

cd /src
if [ ! "$(ls -A /src)" ]; then
    echo "Did you forget mounting your repository into the /src directory."
    echo "Not doing anything!"
    exit 1
fi

echo "Doing the boring stuff of migrating to JUnit5"
git grep -lzP 'import org.junit(?!\.jupiter)' '**.java' | xargs -r0 gawk -i inplace -f /junit5.awk
echo "Done. The following files have changed:"
git --no-pager diff --name-only
