#!/usr/bin/env python
import os
import shutil
import tempfile

import tdx
from plumbum import cmd


this_dir = os.path.dirname(os.path.abspath(__file__))


def test_groovy():
    tmpd = tempfile.mkdtemp()
    try:
        copy_code(this_dir, tmpd)
        run_tests(tmpd)
    finally:
        shutil.rmtree(tmpd)


def copy_code(srcd, dstd):
    shutil.copytree(
        os.path.join(srcd, 'vars'),
        os.path.join(dstd, 'vars'),
    )
    shutil.copy2(
        os.path.join(srcd, 'test.groovy'),
        os.path.join(dstd, 'test.groovy'),
    )
    copy_dir_without_NonCPS(
        os.path.join(srcd, 'src'),
        os.path.join(dstd, 'src'),
    )


def copy_dir_without_NonCPS(srcd, dstd):
    def walker(_, full_dirname, fnames):
        # strip off the srcd abspath prefix (including the trailing '/')
        dirname = full_dirname[len(srcd) + 1:]
        # check whether the file is a regular file, if so copy it
        for f in fnames:
            src = os.path.join(srcd, dirname, f)
            dst = os.path.join(dstd, dirname, f)
            if os.path.isfile(src):
                copy_file_without_NonCPS(src, dst)
    os.path.walk(srcd, walker, None)


def copy_file_without_NonCPS(src, dst):
    dstdir = os.path.dirname(dst)
    if not os.path.isdir(dstdir):
        os.makedirs(dstdir)
    content = tdx.read_content(src)
    output = '\n'.join([
        l for l in content.split('\n')
        if l.strip() != '@NonCPS'
    ])
    tdx.write_content(output, dst)


def run_tests(test_dir):
    classpath = ':'.join([
        os.path.join(test_dir, 'src'),
        os.path.join(test_dir, 'vars')
    ])
    test_script = os.path.join(test_dir, 'test.groovy')
    cmd.groovy['--classpath', classpath, test_script]()


if __name__ == '__main__':
    test_groovy()
