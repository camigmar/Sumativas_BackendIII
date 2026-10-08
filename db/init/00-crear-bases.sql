-- Crea las bases propias de cada microservicio, ademas de banco_xyz (que crea MYSQL_DATABASE).
-- Se ejecuta antes que 01-datos.sql (orden alfabetico) y solo la primera vez, con el volumen vacio.
-- No usa USE: cada script corre en su propia sesion sobre banco_xyz, asi que 01-datos.sql no se ve afectado.
CREATE DATABASE IF NOT EXISTS banco_clientes;
-- CREATE DATABASE IF NOT EXISTS banco_pagos;  -- se habilita en el paso 3 (servicio-pagos)
