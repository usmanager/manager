FROM nginx:alpine

# for htpasswd command
RUN apk add --no-cache --update apache2-utils
RUN rm -f /etc/nginx/conf.d/*

ENV SERVER_NAME docker-api-proxy
ENV PORT 80
ENV CLIENT_MAX_BODY_SIZE 1m
ENV PROXY_READ_TIMEOUT 60s
ENV WORKER_PROCESSES auto

COPY files/run.sh /
COPY files/nginx.conf.tmpl /

# use SIGQUIT for graceful shutdown
# c.f. http://nginx.org/en/docs/control.html
STOPSIGNAL SIGQUIT

RUN chmod +x run.sh
ENTRYPOINT ["/run.sh"]