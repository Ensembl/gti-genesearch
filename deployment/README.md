Requirements
============
`ansible` is best deployed in your own virtual environment to avoid clashes with the EBI farm.

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