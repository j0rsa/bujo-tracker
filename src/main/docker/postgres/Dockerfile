FROM postgres:9.6.6

ADD init /docker-entrypoint-initdb.d
RUN chown postgres:postgres /docker-entrypoint-initdb.d/*
