Requirements
============
`ansible` is best deployed in your own virtual environment to avoid clashes with the EBI farm e.g.
```
virtualenv .
. bin/activate
```

Deployment
==========
To roll out ES and web across the hosts in `hosts`:
```
ansible-playbook deploy.yml -i hosts  
```

To reconfigure and restart ES and web on the hosts in `hosts`:
```
ansible-playbook restart.yml -i hosts  
```

To stop ES and web on the hosts in `hosts`:
```
ansible-playbook stop.yml -i hosts  
```

To remove ES and web installs from the hosts in `hosts`:
```
ansible-playbook clean.yml -i hosts 
```

Note on Java
============
Oracle will only allow downloads of Java with a clickthrough agreement. This is a pain for command line access, so this ansible project uses `curl` to pass a cookie to Oracle to allow download. Note that the URLs are somewhat cryptic and may need updating over time. You can find the URL used in [roles/java/vars/main.yml](roles/java/vars/main.yml). You can find the current URLs from http://www.oracle.com/technetwork/java/javase/downloads/index.html and this topic is discussed more online e.g. https://gist.github.com/hgomez/4697585
