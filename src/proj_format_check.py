#!/usr/bin/python2

import os
import stat
import sys
import tarfile
import tempfile

# Check that a certain file exists within a tar file
def exists(dir_path, filename, tarfile, case_insensitive):
    temppathname = tempfile.mktemp()
    tarfile.extractall(temppathname)
    if case_insensitive:
        if filename.lower() in [x.lower() for x in os.listdir(os.path.join(temppathname,dir_path))]:
            return True, temppathname
        else:
            return (False, '')
    else:
        if os.path.isfile(os.path.join(temppathname, dir_path, filename)):
            return (True, temppathname)
        else:
            return (False, '')

# Exit program if a certain file doesn't exist within a tar file
def exit_if_nonexists(dir_path, filename, tarfile, case_insensitive=False):
    file_exists, pathname = exists(dir_path, filename, tarfile, case_insensitive)
    # print "dir_path, filename, tarfile, case_ins:", dir_path, filename, tarfile, case_insensitive
    # print "file_exists, pathname:", file_exists, pathname
    if file_exists:
        return pathname
    else:
        print "Error: %s not found in archive. Check file and directory names." % (os.path.join(dir_path,filename))
        sys.exit(1)

# Check that a certain file is executable and exit program if not
def exit_if_nonexecutable(full_path):
    executable = stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH
    st = os.stat(full_path)
    mode = st.st_mode
    if not (mode & executable):
        print "Error: did you make your run script executable?"
        sys.exit(1)

def main():

    # 461 submission filenames should all be in this list
    acceptable_filenames = ['proj'+str(x)+'.tar.gz' for x in range(5)]

    # Make sure user specified a filename to test
    if len(sys.argv) < 2:
        print "Error: specify the filename of a file to test."
        sys.exit(1)

    filename = sys.argv[1]

    # Check filename format
    if os.path.basename(filename) not in acceptable_filenames:
        print "Error: filename must be in format projX.tar.gz, where X is the project number."
        sys.exit(1)
    else:
        proj_num = os.path.basename(filename)[4]
        print "Checking format for Project %s..." % proj_num

    # Check that file exists
    if not os.path.isfile(filename):
        print "Error: file doesn't exist."
        sys.exit(1)

    # Check that file's a tarred, gzipped file
    try:
        tarfile.TarFileCompat(filename, 'r', tarfile.TAR_GZIPPED)
        tf = tarfile.open(filename,'r:gz')
    except:
        print "Error opening file; is it compressed with gzip then archived with tar?"
        sys.exit(1)

    # Check that run script exists
    temppath = exit_if_nonexists('proj'+ proj_num + '/','run', tf)

    # Check that run script is executable
    exit_if_nonexecutable(temppath + '/proj' + proj_num + '/run')

    # Check that readme.txt exists
    exit_if_nonexists('proj' + proj_num + '/','README.TXT', tf, True)

    # Project-specific file tests
    if proj_num == '0':
        # No project-specific files need to be checked for Project 0
        pass
    elif proj_num == '1':
        # No project-specific files need to be checked for Project 1
        pass
    elif proj_num == '2':
        # Check that TCP behavior data is included
        if not \
                ( exists('proj' + proj_num + '/','tcp.pdf', tf, False) or \
                        exists('proj' + proj_num + '/', 'tcp.txt', tf, False) ):
            print "Error: didn't find %s or %s!" % ('proj' + proj_num + '/tcp.pdf', 'proj' + proj_num + '/tcp.txt')
            sys.exit(1)
    elif proj_num == '3':
        # No project-specific files need to be checked for Project 3
        pass
    else:
        # Check that user included balances.txt and the block chain binary
        exit_if_nonexists('proj' + proj_num + '/', 'balances.txt', tf, True)
        exit_if_nonexists('proj' + proj_num + '/', 'blockchain.bin', tf, True)

    print "Passed basic tests! Actual project functionality has not been tested."

if __name__ == '__main__':
    main()
