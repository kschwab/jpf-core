#!/usr/bin/env python3

import os
import sys
import grp
import pwd
import argparse

def pysu(userspec: str, command: list=[]) -> None:

    if not os.geteuid() == 0:
        sys.exit("Must run pysu with root privileges.")

    if command == []:
        command = ['/bin/bash']

    if ':' in userspec:
        user, group = userspec.split(':')
    else:
        user, group = (userspec, None)

    if not user.isnumeric():
        uid = int(pwd.getpwnam(user).pw_uid)
    else:
        uid = int(user)

    if group == None:
        gid = int(pwd.getpwuid(uid).pw_gid)
    elif not group.isnumeric():
        gid = int(grp.getgrnam(group).gr_gid)
    else:
        gid = int(group)

    if group == None:
        os.setgroups([gid])
    else:
        os.setgroups(os.getgrouplist(pwd.getpwuid(uid).pw_name, gid))

    environment = os.environ.copy()
    environment['LOGNAME'] = pwd.getpwuid(uid).pw_name
    environment['USER'] = pwd.getpwuid(uid).pw_name
    environment['HOME'] = pwd.getpwuid(uid).pw_dir
    environment['PWD'] = os.getcwd()
    os.setgid(gid)
    os.setuid(uid)

    os.execvpe(command[0], command, env=environment)

if __name__ == '__main__':

    # Create the parser
    parser = argparse.ArgumentParser()
    parser.add_argument( 'userspec', help='A user name (e.g. nobody), or user name and group seperated with colon (e.g. nobody:ftp). Numeric uid/gid values can be used instead of names.')
    parser.add_argument('command', default=['/bin/bash'], nargs='*', help='The command to run. Defaults to /bin/bash if empty.')

    if len(sys.argv) < 2 or sys.argv[1] == '--':
        parser.print_help()
        exit(1)

    args = parser.parse_args()
    pysu(args.userspec, args.command)
