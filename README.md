# ebot

Trading bot for GOLOS Blockchain

## Configuration 

Create working dir on your machine e.g. mkdir -p ~/ebot/db 
Adapt ebotconfig.yml from source code accourding to your needs and copy it to ~/ebot/ directory. 

## Running.

Build docker image. e.g.

```docker build . -t ebot```

Run docker container 

```docker run -v /home/youruser/ebot/db:/db -v /home/youruser/ebot/ebotconfig.yml:/ebotconfig.yml --name ebot ebot```


