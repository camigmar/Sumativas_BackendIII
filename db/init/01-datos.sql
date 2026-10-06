
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `cuentas_interes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cuentas_interes` (
  `cuenta_id` bigint NOT NULL,
  `edad` int DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `saldo` double DEFAULT NULL,
  `saldo_final` double DEFAULT NULL,
  `tipo` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`cuenta_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `cuentas_interes` WRITE;
/*!40000 ALTER TABLE `cuentas_interes` DISABLE KEYS */;
INSERT INTO `cuentas_interes` VALUES (101,30,'John Doe',0,5100,'ahorro'),(102,25,'Jane Smith',7725,8400,'prestamo'),(103,30,'Bob Johnson',11800,12600,'prestamo'),(105,35,'Charlie Green',7000,7210,'hipoteca'),(107,40,'Diana Prince',15000,15750,'prestamo'),(108,80,'Steve Rogers',8400,10200,'ahorro');
/*!40000 ALTER TABLE `cuentas_interes` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `transacciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transacciones` (
  `id` bigint NOT NULL,
  `fecha` date DEFAULT NULL,
  `monto` double DEFAULT NULL,
  `tipo` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `transacciones` WRITE;
/*!40000 ALTER TABLE `transacciones` DISABLE KEYS */;
INSERT INTO `transacciones` VALUES (1,'2024-01-01',1000,'debito'),(2,'2024-01-02',1500,'credito'),(3,'2024-01-03',-200,'debito'),(5,'2024-01-04',800,'credito'),(6,'2024-01-05',700,'debito'),(7,'2024-01-06',1200,'credito'),(8,'2024-01-05',700,'debito'),(9,'2024-01-07',3000,'debito'),(10,'2024-01-08',1000,'credito');
/*!40000 ALTER TABLE `transacciones` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `movimientos_anuales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movimientos_anuales` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cuenta_id` bigint DEFAULT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `fecha` date DEFAULT NULL,
  `monto` double DEFAULT NULL,
  `transaccion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `movimientos_anuales` WRITE;
/*!40000 ALTER TABLE `movimientos_anuales` DISABLE KEYS */;
INSERT INTO `movimientos_anuales` VALUES (1,101,'Ingreso mensual','2024-01-01',1000,'deposito'),(2,101,'Retiro parcial','2024-03-15',-500,'retiro'),(3,102,'Ingreso mensual','2024-05-22',1500,'deposito'),(4,103,'Ingreso mensual','2024-07-10',2000,'deposito'),(5,104,'Compra en tienda','2024-09-05',-100,'compra'),(6,105,'Ingreso extra','2024-10-01',2500,'deposito'),(7,106,'Ingreso mensual','2024-11-20',3000,'deposito'),(8,108,'Ingreso de fin de año','2024-12-31',2000,'deposito'),(9,102,'Ingreso mensual','2024-05-22',1500,'deposito'),(10,101,'Ingreso mensual','2024-01-01',1000,'deposito'),(11,101,'Retiro parcial','2024-03-15',-500,'retiro'),(12,103,'Ingreso mensual','2024-07-10',2000,'deposito'),(13,105,'Ingreso extra','2024-10-01',2500,'deposito'),(14,104,'Compra en tienda','2024-09-05',-100,'compra'),(15,106,'Ingreso mensual','2024-11-20',3000,'deposito'),(16,108,'Ingreso de fin de año','2024-12-31',2000,'deposito'),(17,102,'Ingreso mensual','2024-05-22',1500,'deposito'),(18,101,'Retiro parcial','2024-03-15',-500,'retiro'),(19,101,'Ingreso mensual','2024-01-01',1000,'deposito'),(20,103,'Ingreso mensual','2024-07-10',2000,'deposito'),(21,108,'Ingreso de fin de año','2024-12-31',2000,'deposito'),(22,104,'Compra en tienda','2024-09-05',-100,'compra'),(23,105,'Ingreso extra','2024-10-01',2500,'deposito'),(24,106,'Ingreso mensual','2024-11-20',3000,'deposito');
/*!40000 ALTER TABLE `movimientos_anuales` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `estados_cuenta_anual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estados_cuenta_anual` (
  `cuenta_id` bigint NOT NULL,
  `cantidad_movimientos` int DEFAULT NULL,
  `saldo_neto` double DEFAULT NULL,
  `total_egresos` double DEFAULT NULL,
  `total_ingresos` double DEFAULT NULL,
  PRIMARY KEY (`cuenta_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `estados_cuenta_anual` WRITE;
/*!40000 ALTER TABLE `estados_cuenta_anual` DISABLE KEYS */;
INSERT INTO `estados_cuenta_anual` VALUES (101,6,1500,-1500,3000),(102,3,4500,0,4500),(103,3,6000,0,6000),(104,3,-300,-300,0),(105,3,7500,0,7500),(106,3,9000,0,9000),(108,3,6000,0,6000);
/*!40000 ALTER TABLE `estados_cuenta_anual` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

