# Proyecto-planIMRT

Proyecto Integrador de Ingeniería de Software - Licenciatura en Bioinformática - FIUNER
Basado en el Proyecto Final de grado de Laura Osuna.

## Tabla de Contenidos

- [Descripción](#descripción)
- [Equipo](#equipo)
- [Estructura](#estructura-del-proyecto)

## Descripción

En la Radioterapia de Intensidad Modulada (IMRT) hay una alta cantidad de campos de tratamiento que no pasan el control de calidad. Esto implica que el físico médico a cargo de la planificación de tratamiento debe realizar nuevamente la planificación y problarlo. Es por ello que se propone un sistema que, a partir de el archivo con la planificación, calcule métricas que ayuden al usuario a tomar decisiones sobre el plan de tratamiento.

## Equipo

- [MartinaBoero73](https://github.com/MartinaBoero73) — Responsable del Repositorio
- [IvanaGasco](https://github.com/IvanaGasco) — Colaboradora
- [CMacchi25](https://github.com/CMacchi25) — Colaboradora

## Estructura del proyecto

```plaintext
proyecto-planIMRT/
│
├── planIMRT/                # Implementación principal del sistema (aplicación Spring Boot)
│   └── src/
|       ├── main/
|       |   ├── java/com.planimrt/
|       |   |   ├── PlanImrtApplication.java # Aplicacion principal del proyecto
|       |   |   ├── controllers/             # Controladores REST para manejo de peticiones
|       |   |   ├── DTOs/                    # Objetos de transferencia de datos entre capas 
|       |   |   ├── forms/                   # Clases para formularios o requests del usuario 
|       |   |   ├── model/                   # Entidades del dominio 
|       |   |   ├── repo/                    # Interfaces de repositorio para acceso a la BD
|       |   |   ├── security/                # Configuración de autenticación/autorización 
|       |   |   └── services/                # Lógica de negocio
|       |   |
|       |   └── resources/
|       |       ├── templates/               # Vistas HTML 
|       |       └── static/                  # Archivos estáticoservidos directamente
|       |
|       └── test/                            # Tests unitarios e integrados del proyecto
|
| 
├── codigo/                              # Implementación del proyecto
│   └── modulos/                         # Scripts y componentes reutilizables
|
├── diseño/                                       # Material de diseño y modelado del sistema
│   ├── arquitectura/                             # Documentos y descripciones de la arquitectura
│   └── diagramas/                                # Diagramas UML, contexto, secuencia, comp
|       ├── clases/                               # Diagramas de clases
|       |   └── DC_autenticacion.png              # Diagrama de clases para autenticación
|       |   ├── DC_GUI.png                        # Diagrama de clases interfaz
|       |   └── DC_procesamiento                  # Diagrama de clases para procesamiento
│       ├── diagrama_de_dominio.png               # Modelo de dominio
|       ├── diagrama_de_casos_de_uso.png          # Diagrama de casos de uso
│       ├── diagrama_de_componentes.png           # Diagrama de componentes
│       ├── diagrama_de_contenedores.png          # Diagrama de contenedores
│       ├── diagrama_de_contexto_N0.png           # Contexto de alto nivel (nivel 0)
│       ├── diagrama_de_contexto_N1.png           # Contexto detallado (nivel 1)
│       ├── diagrama_de_despliegue.png            # Diagrama de despliegue de UML
│       ├── diagrama_de_secuencia_seguridad.png   # Diagrama de Secuencia para caso de seguridad
│       └── diagrama_de_secuencia_usabilidad.png  # Diagrama de Secuencia para caso de usabilidad
|
├── documentacion/                      # Documentación formal del proyecto
│   ├── especificaciones/               # Especificaciones técnicas y funcionales
│   └── requerimientos/                 # Requerimientos del sistema
│       ├── casos_de_uso.xlsx           # Lista de casos de uso en formato tabular
│       └── escenarios_de_calidad.xlsx  # Escenarios de atributos de calidad
|
├── practicas/
│   ├── TP1- Modulo1.pdf                  # Informe del trabajo practico 1 modulo 1
|   ├── Trabajo Práctico M1 - parte2.pdf  # Informe del trabajo practico 1 modulo 2
|   ├── Trabajo Práctico Nº 2.pdf         # Informe del trabajo practico 2
|   └── Trabajo Práctico Nº 3.pdf         # Informe del trabajo practico 3
|
└── recursos/                           # Archivos auxiliares y de soporte
    ├── imagenes/                       # Imágenes varias para documentación y diseño
    └── plantillas/                     # Plantillas para interfaz web

```

## Elementos de Configuración

Total de CIs: 6

- Documentación:  2 CIs
- Diseño: 4 CIs

## Última actualización

1/9/2025 - v2.0
