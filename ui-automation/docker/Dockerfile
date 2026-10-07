FROM php:8.1-apache@sha256:72f0c0134922985022d55a39b0390d35dc7a10e961aa6c44fa35b4c3102f4268
RUN apt-get update && apt-get install -y --no-install-recommends libzip-dev libpng-dev libjpeg62-turbo-dev libfreetype6-dev curl unzip \
 && docker-php-ext-configure gd --with-freetype --with-jpeg \
 && docker-php-ext-install mysqli gd zip && rm -rf /var/lib/apt/lists/*
ARG OC_COMMIT=8e69d02a19cd709779a57ac0206e5ede50814e1b
RUN curl -fsSL https://codeload.github.com/phpredis/phpredis/tar.gz/refs/tags/6.1.0 -o /tmp/redis.tgz \
 && tar xzf /tmp/redis.tgz -C /tmp && cd /tmp/phpredis-6.1.0 \
 && phpize && ./configure && make -j2 && make install && docker-php-ext-enable redis \
 && rm -rf /tmp/redis.tgz /tmp/phpredis-6.1.0
RUN curl -fsSL https://codeload.github.com/opencart/opencart/zip/${OC_COMMIT} -o /tmp/oc.zip \
 && unzip -q /tmp/oc.zip -d /tmp && cp -a /tmp/opencart-${OC_COMMIT}/upload/. /var/www/html/ \
 && rm -rf /tmp/oc.zip /tmp/opencart-${OC_COMMIT}
COPY bootstrap.php /opt/qa/bootstrap.php
COPY entrypoint.sh /opt/qa/entrypoint.sh
RUN chmod +x /opt/qa/entrypoint.sh && chown -R www-data:www-data /var/www/html
ENTRYPOINT ["/opt/qa/entrypoint.sh"]
CMD ["apache2-foreground"]
