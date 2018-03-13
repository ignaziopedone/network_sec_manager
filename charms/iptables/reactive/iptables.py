from charmhelpers.core.hookenv import (
    action_get,
    action_fail,
    action_set,
    config,
    status_set,
)

from charms.reactive import (
    remove_state as remove_flag,
    set_state as set_flag,
    when,
)
import charms.sshproxy
from subprocess import (
    Popen,
    CalledProcessError,
    PIPE,
)


cfg = config()


@when('config.changed')
def config_changed():
    if all(k in cfg for k in ['mode']):
        if cfg['mode'] in ['yes', 'no']:
            set_flag('iptables.configured')
            status_set('active', 'ready!')
            return
    status_set('blocked', 'Waiting for configuration')



@when('iptables.configured')
@when('actions.pass-files')
def pass_files():
    try:
        # Enter the command to stop your service(s)
        cmd = "sudo chmod 777 -R ./"
        result, err = charms.sshproxy._run(cmd)
        cmd = "scp -i chiavefile.pem -o \"StrictHostKeyChecking no\" ignaziopedone@130.192.1.95:/Users/ignaziopedone/configurazioni/iptables.sh /home/ubuntu"
        result, err = charms.sshproxy._run(cmd)
    except Exception as e:
        action_fail('command failed: {}, errors: {}'.format(e, e.output))
    else:
        action_set({'stdout': result,
                    'errors': err})
    finally:
        remove_flag('actions.pass-files')


@when('iptables.configured')
@when('actions.start-configuration')
def start_configuration():
    try:
        # Enter the command to restart your service(s)
        # This cmd run the user configuration script
        cmd = "source iptables.sh" 
        result, err = charms.sshproxy._run(cmd)
    except Exception as e:
        action_fail('command failed: {}, errors: {}'.format(e, e.output))
    else:
        action_set({'stdout': result,
                    'errors': err})
    finally:
        remove_flag('actions.start-configuration')

@when('iptables.configured')
@when('actions.start')
def start():
    try:
        # Enter the command to restart your service(s)
        # This allows to configure the newtwork interfaces and the SFC
        cmd = "source network.sh" 
        result, err = charms.sshproxy._run(cmd)
        cmd = "source script.sh"
        result, err = charms.sshproxy._run(cmd)
    except Exception as e:
        action_fail('command failed: {}, errors: {}'.format(e, e.output))
    else:
        action_set({'stdout': result,
                    'errors': err})
    finally:
        remove_flag('actions.start')

def format_curl(method, path, data=None):
    """ A utility function to build the curl command line. """

    # method must be GET or POST
    if method not in ['GET', 'POST']:
        # Throw exception
        return None

    # Get our service info
    host = '127.0.0.1'
    port = get_port()
    mode = cfg['mode']

    cmd = ['curl',
           # '-D', '/dev/stdout',
           '-H', 'Accept: application/vnd.yang.data+xml',
           '-H', 'Content-Type: application/vnd.yang.data+json',
           '-X', method]

    if method == "POST" and data:
        cmd.append('-d')
        cmd.append('{}'.format(data))

    cmd.append(
        'http://{}:{}/api/v1/{}{}'.format(host, port, mode, path)
    )
    return cmd
