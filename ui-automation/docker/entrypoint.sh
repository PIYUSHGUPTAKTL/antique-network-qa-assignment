#!/bin/sh
set -eu
php /opt/qa/bootstrap.php
exec docker-php-entrypoint "$@"
