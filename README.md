# Proyecto-planIMRT

![Tests](https://github.com/MartinaBoero73/proyecto-planIMRT/workflows/Tests/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)

Proyecto Integrador de Ingeniería de Software - Licenciatura en Bioinformática - FIUNER
Basado en el Proyecto Final de grado de Laura Osuna.

## Tabla de Contenidos

- [Descripción](#descripción)
- [Equipo](#equipo)
- [Estructura](#estructura-del-proyecto)

## Descripción

En la Radioterapia de Intensidad Modulada (IMRT) hay una alta cantidad de campos de tratamiento que no pasan el control de calidad. Esto implica que el físico médico a cargo de la planificación de tratamiento debe realizar nuevamente la planificación y problarlo. Es por ello que se propone un sistema que, a partir de el archivo con la planificación, calcule métricas que ayuden al usuario a tomar decisiones sobre el plan de tratamiento.

### Características

| Característica                    | Estado           | Versión |
|-----------------------------------|------------------|---------|
| Interfaz de login y registro      | ✅ Completado     | v2.0    |
| Cálculo de índice MCS             | ✅ Completado     | v2.0    |
| Carga de archivos DICOM           | ✅ Completado     | v2.0    |
| Cálculo de índice MCS             | ✅ Completado     | v2.0    |
| Visualización de gráficos         | 🚧 En desarrollo | v2.1    |
| Exportación de resultados         | 🚧 En desarrollo | v2.2    |
| Consulta y filtrado de resultados | 📋 Planificado   | v3.0    |
| Dashboard de estadísticas         | 📋 Planificado   | v3.0    |
| Pefril de investigador            | 📋 Planificado    | v3.0    |
| API REST pública                  | 📋 Planificado    | v3.0    |

Próximos pasos: despliegue en Railway 

## Equipo

- [MartinaBoero73](https://github.com/MartinaBoero73) — Responsable del Repositorio
- [IvanaGasco](https://github.com/IvanaGasco) — Colaboradora
- [CMacchi25](https://github.com/CMacchi25) — Colaboradora

## Estructura del proyecto

```plaintext
proyecto-planIMRT/
│
├── planIMRT/                                   # Implementación principal del sistema (aplicación Spring Boot)
│   └── src/
|       ├── main/
|       |   ├── java/com.planimrt/
|       |   |   ├── PlanImrtApplication.java    # Aplicacion principal del proyecto
|       |   |   ├── controllers/                # Controladores REST para manejo de peticiones
|       |   |   ├── DTOs/                       # Objetos de transferencia de datos entre capas 
|       |   |   ├── forms/                      # Clases para manejar formularios o requests del usuario 
|       |   |   ├── model/                      # Entidades del dominio 
|       |   |   ├── repo/                       # Interfaces de repositorio para acceso a la base de datos
|       |   |   ├── security/                   # Configuración de autenticación/autorización 
|       |   |   └── services/                   # Lógica de negocio
|       |   |
|       |   └── resources/
|       |       ├── application.properties      # configuraciones del proyecto
|       |       ├── dicom/
|       |       ├── dicom_text/                 # Archivos de prueba
|       |       ├── templates/                  # Vistas HTML 
|       |       └── static/                     # Archivos estáticoservidos directamente
|       |
|       └── test/                        
|           ├── java/com.planimrt/              # Tests de integracion del proyecto 
|           └── resources/                      # Recursos para los tests
|
├── documentacion/                              # Documentación formal del proyecto
|    ├── especificaciones/                      # Especificaciones técnicas y funcionales
|    └── requerimientos/                        # Requerimientos del sistema
|        ├── casos_de_uso.xlsx                  # Lista de casos de uso en formato tabular
|        └── escenarios_de_calidad.xlsx         # Escenarios de atributos de calidad
| 
├── diseño/     
|    ├── arquitectura/                           
|    └── diagramas/                             # Diagramas de vistas C4, Casos de Uso, despliegue, secuencia y clases
|
├── practicas/                                  # Archivos PDF de resolución de actividades prácticas
|
├── .github/workflows/tests.yml                 # Workflow para automatización de tests con Github Actions 
├── pom.xml                                     # Configuracion de proyecto, dependencias y build                        
├── INVENTARIO_CIS.md                           # Archivo con ítems de configuracion inventariados
└── README.md                                   # Este archivo :D

```

## Elementos de Configuración

Total de CIs: 6

- Documentación:  2 CIs
- Diseño: 4 CIs

## Última actualización

1/11/2025 - v2.0
