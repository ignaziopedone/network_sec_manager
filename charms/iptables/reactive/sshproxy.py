##
# Copyright 2016 Canonical Ltd.
# All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
##

from charmhelpers.core.hookenv import (
    action_fail,
    action_get,
    action_set,
    config,
)
from charms.reactive import (
    remove_state,
    set_state,
    when,
    when_not,
)
import charms.sshproxy
import os
import paramiko
import subprocess


@when('config.changed')
def ssh_configured():
    """Check if charm is properly configured.

    Check to see if the charm is configured with SSH credentials. If so,
    set a state flag that can be used to execute ssh-only actions.

    For example:

    @when('sshproxy.configured')
    def run_remote_command(cmd):
        ...

    @when_not('sshproxy.configured')
    def run_local_command(cmd):
        ...
    """
    cfg = config()
    if all(k in cfg for k in ['ssh-hostname', 'ssh-username',
                              'ssh-password', 'ssh-private-key']):
        set_state('sshproxy.configured')
    else:
        remove_state('sshproxy.configured')


def generate_ssh_key():
    """Generate a new 4096-bit rsa keypair.

    If there is an existing keypair for this unit, it will be overwritten.
    """
    cfg = config()
    if all(k in cfg for k in ['ssh-key-type', 'ssh-key-bits']):
        keytype = cfg['ssh-key-type']
        bits = str(cfg['ssh-key-bits'])
        privatekey = '/root/.ssh/id_juju_sshproxy'
        publickey = "{}.pub".format(privatekey)

        if os.path.exists(privatekey):
            os.remove(privatekey)
        if os.path.exists(publickey):
            os.remove(publickey)

        cmd = "ssh-keygen -t {} -b {} -N '' -f {}".format(
            keytype,
            bits,
            privatekey
        )

        output, err = charms.sshproxy.run_local([cmd])
        if len(err) == 0:
            return True
    return False


@when('actions.generate-ssh-key')
def action_generate_ssh_key():
    """Generate a new 4096-bit rsa keypair.

    If there is an existing keypair for this unit, it will be overwritten.
    """
    try:
        if not generate_ssh_key():
            action_fail('Unable to generate ssh key.')
    except subprocess.CalledProcessError as e:
        action_fail('Command failed: %s (%s)' %
                    (' '.join(e.cmd), str(e.output)))
    finally:
        remove_state('actions.generate-ssh-key')


def get_ssh_public_key():
    """Get the public SSH key of this unit."""
    publickey_path = '/root/.ssh/id_juju_sshproxy.pub'
    publickey = None
    if os.path.exists(publickey_path):
        with open(publickey_path, 'r') as f:
            publickey = f.read()

    return publickey


@when('actions.get-ssh-public-key')
def action_get_ssh_public_key():
    """Get the public SSH key of this unit."""
    try:
        action_set({'pubkey': get_ssh_public_key()})
    except subprocess.CalledProcessError as e:
        action_fail('Command failed: %s (%s)' %
                    (' '.join(e.cmd), str(e.output)))
    finally:
        remove_state('actions.get-ssh-public-key')


@when('actions.verify-ssh-credentials')
def verify_ssh_credentials():
    """Verify the ssh credentials have been installed to the VNF.

    Attempts to run a stock command - `hostname` on the remote host.
    """
    try:
        cfg = config()
        if len(cfg['ssh-hostname']) and len(cfg['ssh-username']):
            cmd = 'hostname'
            output, err = charms.sshproxy._run(cmd)

            if len(err):
                action_fail("Command '{}' returned error code {}".format(
                    cmd,
                    err,
                ))
            else:
                action_set({'output': output})
        else:
            action_fail('Invalid SSH credentials.')
    except subprocess.CalledProcessError as e:
        action_fail('Command failed: %s (%s)' %
                    (' '.join(e.cmd), str(e.output)))
    except paramiko.ssh_exception.AuthenticationException as e:
        action_fail('Authentication failed.')
        pass
    except paramiko.ssh_exception.BadAuthenticationType as e:
        action_fail('{}'.format(e.explanation))
    except paramiko.ssh_exception.BadHostKeyException as e:
        action_fail('Host key mismatch: expected {} but got {}.'.format(
            e.expected_key,
            e.got_key,
        ))
    finally:
        remove_state('actions.verify-ssh-credentials')


@when('actions.run')
def run_command():
    """Run an arbitrary command.

    Run an arbitrary command, either locally or over SSH with the configured
    credentials.
    """
    try:
        cmd = action_get('command')
        output, err = charms.sshproxy._run(cmd)
        if len(err):
            action_fail("Command '{}' returned error code {}".format(cmd, err))
        else:
            action_set({'output': output})
    except subprocess.CalledProcessError as e:
        action_fail('Command failed: %s (%s)' %
                    (' '.join(e.cmd), str(e.output)))
    finally:
        remove_state('actions.run')


def charm_dir():
    """Return the root directory of the current charm."""
    d = os.environ.get('JUJU_CHARM_DIR')
    if d is not None:
        return d
    return os.environ.get('CHARM_DIR')


def run_local(cmd, env=None):
    """Run a command locally."""
    if isinstance(cmd, str):
        cmd = cmd.split(' ') if ' ' in cmd else [cmd]

    p = Popen(cmd,
              env=env,
              shell=True,
              stdout=PIPE,
              stderr=PIPE)
    stdout, stderr = p.communicate()
    retcode = p.poll()
    if retcode > 0:
        raise CalledProcessError(returncode=retcode,
                                 cmd=cmd,
                                 output=stderr.decode("utf-8").strip())
    return (stdout.decode('utf-8').strip(), stderr.decode('utf-8').strip())


@when_not('sshproxy.installed')
def install_vnf_ubuntu_proxy():
    """Install and Configure SSH Proxy."""
    generate_ssh_key()
    set_state('sshproxy.installed')
