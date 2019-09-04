# Overview
This document describes how to use `ansible` to deploy a cluster of Elastic nodes and a search REST front end. 

# Requirements
`ansible` is best deployed in your own virtual environment to avoid clashes with the EBI farm e.g.
```
virtualenv .
source ./bin/activate
```

# Configuration
To use ansible, you need the following files:
* `hosts` - groups of hosts that play different roles. The groups are:
    * `es-data` - nodes hosting Elastic data
    * `es-head` - a non-data nodes that acts as entry point to the cluster
    * `web-head` - the web application
* `vars.yml` - variables used in deployment and to generate an `application.properties` file. See `vars.yml.example`

# Deployment
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

Note that older versions of the playbook installed the Elasticsearch `head` plugin. This is now supported as a Chrome extension, available from https://chrome.google.com/webstore/detail/elasticsearch-head/ffmkiejjmecolpfloofpjologoblkegm

## Known issues
### Java downloads
Oracle will only allow downloads of Java with a click through agreement. This is a pain for command line access, so this ansible project uses `curl` to pass a cookie to Oracle to allow download. Note that the URLs are somewhat cryptic and may need updating over time. You can find the URL used in [roles/java/vars/main.yml](roles/java/vars/main.yml). You can find the current URLs from http://www.oracle.com/technetwork/java/javase/downloads/index.html and this topic is discussed more online e.g. https://gist.github.com/hgomez/4697585

An alternative is to use OpenJDK Java. However, any change of JDK needs careful testing as performance and functionality can reportedly be different (this was certainly true in the past).


